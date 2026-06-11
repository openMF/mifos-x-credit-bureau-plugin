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
 *   If both null: rfc=null → CDC-001
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

    public ClientApiService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    private static final ParameterizedTypeReference<List<FineractClientIdentifierResponse>>
            IDENTIFIER_LIST_TYPE = new ParameterizedTypeReference<>() {};

    public ClientData getClientData(Long clientId) {
        HttpEntity<String> entity = new HttpEntity<>(buildHeaders());

        // Step 1 — basic client data
        FineractClientResponse apiResponse =
                fetchClientBasic(clientId, entity);

        // Step 2 — RFC from identifiers
        // Primary: NATIONAL_ID from /identifiers
        // Fallback: externalId
        String rfc = fetchRfc(clientId, entity,
                apiResponse.getExternalId());
        log.info("RFC present for clientId {}: {}",
                clientId, rfc != null);

        // Step 3 — address data
        FineractClientAddressResponse firstAddress =
                fetchAddress(clientId, entity);

        List<String> streetAddress = Stream.of(
                        firstAddress != null
                                ? firstAddress.getAddressLine1() : null,
                        firstAddress != null
                                ? firstAddress.getAddressLine2() : null,
                        firstAddress != null
                                ? firstAddress.getAddressLine3() : null
                )
                .filter(s -> s != null && !s.isBlank())
                .collect(Collectors.toList());

        return ClientData.builder()
                .id(apiResponse.getId())
                .firstName(apiResponse.getFirstname())
                .lastName(apiResponse.getLastname())
                .externalId(apiResponse.getExternalId())
                .rfc(rfc)
                .dateOfBirth(apiResponse.getDateOfBirth())
                .phoneNumber(apiResponse.getMobileNo())
                .emailAddress(apiResponse.getEmailAddress())
                .addressType(firstAddress != null
                        ? firstAddress.getAddressType() : null)
                .addressId(firstAddress != null
                        ? firstAddress.getId() : null)
                .streetAddress(streetAddress)
                .townVillage(firstAddress != null
                        ? firstAddress.getTownVillage() : null)
                .city(firstAddress != null
                        ? firstAddress.getCity() : null)
                .stateProvince(firstAddress != null
                        ? firstAddress.getStateName() : null)
                .country(firstAddress != null
                        ? firstAddress.getCountryName() : null)
                .postalCode(firstAddress != null
                        ? firstAddress.getPostalCode() : null)
                .build();
    }

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
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to fetch client from Fineract for clientId="
                            + clientId, e);
        }
    }

    /**
     * Fetch RFC from GET /clients/{id}/identifiers.
     * Looks for documentType=NATIONAL_ID.
     * Falls back to externalId if not found.
     * Returns null if both missing.
     * Never logs RFC value — only presence.
     */
    private String fetchRfc(Long clientId,
            HttpEntity<String> entity, String externalId) {
        String identifiersUrl = fineractApiClientBaseUrl
                + clientId + "/identifiers";
        try {
            ResponseEntity<List<FineractClientIdentifierResponse>>
                    response = restTemplate.exchange(
                            identifiersUrl, HttpMethod.GET,
                            entity, IDENTIFIER_LIST_TYPE);

            if (response.getBody() != null) {
                String rfc = response.getBody().stream()
                        .filter(FineractClientIdentifierResponse
                                ::isNationalId)
                        .map(FineractClientIdentifierResponse
                                ::getDocumentKey)
                        .findFirst()
                        .orElse(null);

                if (rfc != null) return rfc;
            }
        } catch (HttpClientErrorException.NotFound e) {
            log.debug("No identifiers found for clientId: {}",
                    clientId);
        } catch (Exception e) {
            log.warn("Could not fetch identifiers for clientId: {} — {}",
                    clientId, e.getMessage());
        }

        // Fallback to externalId
        if (externalId != null && !externalId.isBlank()) {
            log.debug("Using externalId as RFC fallback for clientId: {}",
                    clientId);
            return externalId;
        }

        return null;
    }

    private FineractClientAddressResponse fetchAddress(
            Long clientId, HttpEntity<String> entity) {
        String url = fineractApiClientAddressBaseUrl
                + clientId + "/addresses";
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
