package org.mifos.creditbureau.service;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.client.FineractClientAddressResponse;
import org.mifos.creditbureau.data.client.FineractClientIdentifierResponse;
import org.mifos.creditbureau.data.client.FineractClientResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Fetches client data from Apache Fineract.
 *
 * Phase 2 fix: now calls all 3 endpoints:
 *   GET /clients/{id}             → basic data
 *   GET /clients/{id}/identifiers → RFC (NATIONAL_ID)
 *   GET /clients/{id}/addresses   → address data
 *
 * RFC strategy:
 *   Primary:  GET /identifiers where documentType=NATIONAL_ID
 *   Fallback: externalId from GET /clients/{id}
 *   If both null: rfc=null — CDC-001 on real CDC call
 *
 * Security:
 *   Never logs RFC value — only "RFC present: true/false"
 */
@Slf4j
@Service
public class ClientApiService {

    @Value("${mifos.fineract.api.base-url.client}")
    private String fineractApiClientBaseUrl;

    @Value("${mifos.fineract.api.base-url.address}")
    private String fineractApiClientAddressBaseUrl;

    @Value("${mifos.fineract.api.username}")
    private String username;

    @Value("${mifos.fineract.api.password}")
    private String password;

    private final RestTemplate restTemplate;

    private static final ParameterizedTypeReference<List<FineractClientIdentifierResponse>>
            IDENTIFIER_LIST_TYPE = new ParameterizedTypeReference<>() {};

    public ClientApiService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Fetches and aggregates client data from all 3 Fineract endpoints.
     *
     * Calls GET /clients/{id}, GET /clients/{id}/identifiers,
     * and GET /clients/{id}/addresses in sequence.
     *
     * @param clientId Fineract client ID — must not be null
     * @return aggregated ClientData with RFC, address, and personal info
     * @throws IllegalStateException if Fineract returns empty response
     */
    public ClientData getClientData(Long clientId) {
        HttpEntity<String> entity = new HttpEntity<>(buildHeaders());
        FineractClientResponse apiResponse = fetchClientBasic(clientId, entity);
        String rfc = fetchRfc(clientId, entity, apiResponse.getExternalId());
        log.info("RFC present for clientId {}: {}", clientId, rfc != null);
        FineractClientAddressResponse firstAddress = fetchAddress(clientId, entity);
        List<String> streetAddress = buildStreetAddress(firstAddress);
        return buildClientData(apiResponse, rfc, firstAddress, streetAddress);
    }

    /**
     * Builds ClientData record from all fetched Fineract response parts.
     *
     * @param apiResponse   basic client data from Fineract
     * @param rfc           extracted RFC or null
     * @param address       first address or null
     * @param streetAddress assembled street address lines
     * @return populated ClientData record
     */
    private ClientData buildClientData(
            FineractClientResponse apiResponse,
            String rfc,
            FineractClientAddressResponse address,
            List<String> streetAddress) {
        return ClientData.builder()
                .id(apiResponse.getId())
                .firstName(apiResponse.getFirstname())
                .lastName(apiResponse.getLastname())
                .externalId(apiResponse.getExternalId())
                .rfc(rfc)
                .dateOfBirth(apiResponse.getDateOfBirth())
                .phoneNumber(apiResponse.getMobileNo())
                .emailAddress(apiResponse.getEmailAddress())
                .addressType(address != null ? address.getAddressType() : null)
                .addressId(address != null ? address.getId() : null)
                .streetAddress(streetAddress)
                .townVillage(address != null ? address.getTownVillage() : null)
                .city(address != null ? address.getCity() : null)
                .stateProvince(address != null ? address.getStateName() : null)
                .country(address != null ? address.getCountryName() : null)
                .postalCode(address != null ? address.getPostalCode() : null)
                .build();
    }

    /**
     * Assembles street address lines from Fineract address response.
     *
     * @param address Fineract address response or null
     * @return non-blank address lines as list, empty list if address null
     */
    private List<String> buildStreetAddress(
            FineractClientAddressResponse address) {
        return Stream.of(
                        address != null ? address.getAddressLine1() : null,
                        address != null ? address.getAddressLine2() : null,
                        address != null ? address.getAddressLine3() : null)
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * Fetches basic client data from GET /clients/{id}.
     *
     * @param clientId Fineract client ID
     * @param entity   HTTP entity with auth headers
     * @return FineractClientResponse with basic client fields
     * @throws IllegalStateException if Fineract returns null body
     */
    private FineractClientResponse fetchClientBasic(
            Long clientId, HttpEntity<String> entity) {
        String url = fineractApiClientBaseUrl + clientId;
        try {
            ResponseEntity<FineractClientResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET,
                            entity, FineractClientResponse.class);
            if (response.getBody() == null) {
                throw new IllegalStateException(
                        "Fineract returned empty client for clientId="
                                + clientId);
            }
            return response.getBody();
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to fetch client from Fineract for clientId="
                            + clientId, e);
        }
    }

    /**
     * Fetches RFC by trying NATIONAL_ID from identifiers first,
     * then falling back to externalId.
     *
     * @param clientId   Fineract client ID
     * @param entity     HTTP entity with auth headers
     * @param externalId fallback RFC source from basic client data
     * @return RFC string or null if neither source has it
     */
    private String fetchRfc(Long clientId,
            HttpEntity<String> entity, String externalId) {
        String rfcFromIdentifiers = findNationalIdFromIdentifiers(
                clientId, entity);
        if (rfcFromIdentifiers != null) return rfcFromIdentifiers;
        return fallbackToExternalId(clientId, externalId);
    }

    /**
     * Calls GET /clients/{id}/identifiers and extracts NATIONAL_ID documentKey.
     *
     * @param clientId Fineract client ID
     * @param entity   HTTP entity with auth headers
     * @return RFC from NATIONAL_ID identifier or null if not found
     */
    private String findNationalIdFromIdentifiers(
            Long clientId, HttpEntity<String> entity) {
        String url = fineractApiClientBaseUrl + clientId + "/identifiers";
        try {
            ResponseEntity<List<FineractClientIdentifierResponse>> response =
                    restTemplate.exchange(url, HttpMethod.GET,
                            entity, IDENTIFIER_LIST_TYPE);
            if (response.getBody() == null) return null;
            return response.getBody().stream()
                    .filter(FineractClientIdentifierResponse::isNationalId)
                    .map(FineractClientIdentifierResponse::getDocumentKey)
                    .findFirst()
                    .orElse(null);
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No identifiers found for clientId: {}", clientId);
            return null;
        } catch (Exception e) {
            log.warn("Could not fetch identifiers for clientId: {} — {}",
                    clientId, e.getMessage());
            return null;
        }
    }

    /**
     * Returns externalId as RFC fallback if present and non-blank.
     *
     * @param clientId   Fineract client ID for logging
     * @param externalId externalId from basic client data
     * @return externalId if not blank, otherwise null
     */
    private String fallbackToExternalId(Long clientId, String externalId) {
        if (externalId != null && !externalId.isBlank()) {
            log.debug("Using externalId as RFC fallback for clientId: {}",
                    clientId);
            return externalId;
        }
        return null;
    }

    /**
     * Fetches first address from GET /clients/{id}/addresses.
     *
     * @param clientId Fineract client ID
     * @param entity   HTTP entity with auth headers
     * @return first address or null if none found
     */
    private FineractClientAddressResponse fetchAddress(
            Long clientId, HttpEntity<String> entity) {
        String url = fineractApiClientAddressBaseUrl + clientId + "/addresses";
        try {
            ResponseEntity<FineractClientAddressResponse[]> response =
                    restTemplate.exchange(url, HttpMethod.GET,
                            entity, FineractClientAddressResponse[].class);
            FineractClientAddressResponse[] body = response.getBody();
            return (body != null && body.length > 0) ? body[0] : null;
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No address found for clientId: {}", clientId);
            return null;
        } catch (Exception e) {
            log.warn("Could not fetch address for clientId: {} — {}",
                    clientId, e.getMessage());
            return null;
        }
    }

    /**
     * Builds HTTP headers with Basic Auth and Fineract tenant ID.
     *
     * @return configured HttpHeaders
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization",
                getBasicAuthenticationHeader(username, password));
        headers.add("fineract-platform-tenantid", "default");
        return headers;
    }

    private String getBasicAuthenticationHeader(
            String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + java.util.Base64.getEncoder()
                .encodeToString(credentials.getBytes());
    }
}
