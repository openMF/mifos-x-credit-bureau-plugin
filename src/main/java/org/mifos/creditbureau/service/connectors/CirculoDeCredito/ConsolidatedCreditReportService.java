package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.CBCreditReportData;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRCCRequest;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoResponse;
import org.mifos.creditbureau.mappers.CirculoDeCreditoResponseToCBCreditReportDataMapper;
import org.mifos.creditbureau.service.ClientApiService;
import org.mifos.creditbureau.service.registration.CreditBureauRegistrationReadImplService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConsolidatedCreditReportService {

    @Value("${mifos.circulodecredito.base.url}")
    private String baseUrl;

    @Value("${mifos.circulodecredito.mock.enabled:false}")
    private boolean mockEnabled;

    private final CreditBureauRegistrationReadImplService
            creditBureauRegistrationReadService;
    private final ClientApiService clientApiService;
    private final CirculoDeCreditoResponseToCBCreditReportDataMapper
            responseMapper;
    private final RestTemplate restTemplate;

    public ConsolidatedCreditReportService(
            CreditBureauRegistrationReadImplService creditBureauRegistrationReadService,
            ClientApiService clientApiService,
            CirculoDeCreditoResponseToCBCreditReportDataMapper responseMapper,
            RestTemplateBuilder restTemplateBuilder) {
        this.creditBureauRegistrationReadService =
                creditBureauRegistrationReadService;
        this.clientApiService = clientApiService;
        this.responseMapper = responseMapper;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Fetches consolidated credit report from Circulo de Credito
     * using real client data from Apache Fineract.
     */
    public CBCreditReportData getConsolidatedCreditReport(
            Long creditBureauId,
            Long clientId) throws Exception {

        if (creditBureauId == null) {
            throw new IllegalArgumentException(
                    "Credit bureau ID must not be null"
            );
        }
        if (clientId == null) {
            throw new IllegalArgumentException(
                    "Client ID must not be null"
            );
        }

        // Mock mode for local development
        if (mockEnabled) {
            log.info("Mock mode enabled — returning mock credit report");
            return CBCreditReportData.builder()
                    .bureauName("Circulo de Credito (Mock)")
                    .country("MX")
                    .build();
        }

        // Step 1 — Fetch real client data from Fineract
        log.info("Fetching Fineract client data for clientId: {}",
                clientId);
        ClientData clientData = clientApiService.getClientData(clientId);

        // Step 2 — Build CDC request from real client data
        CirculoDeCreditoRCCRequest request = buildRequest(clientData);

        // Step 3 — Get bureau API credentials
        Map<String, String> keys = creditBureauRegistrationReadService
                .getRegistrationParamMap(creditBureauId);
        String apiKey = keys.get("x-api-key");

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "x-api-key not configured for bureau ID: "
                            + creditBureauId
            );
        }

        // Step 4 — Build request headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<CirculoDeCreditoRCCRequest> entity =
                new HttpEntity<>(request, headers);

        // Step 5 — Send request to CDC production endpoint
        String url = baseUrl + "v1/rcc";
        log.info("Sending RCC request to CDC for clientId: {}",
                clientId);

        try {
            ResponseEntity<CirculoDeCreditoResponse> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.POST,
                            entity,
                            CirculoDeCreditoResponse.class
                    );

            log.info("CDC RCC response status: {}",
                    response.getStatusCode());

            // Step 6 — Map CDC response to standard CBCreditReportData
            CBCreditReportData result = responseMapper
                    .toCBCreditReportData(response.getBody());

            log.info("Successfully mapped CDC response for clientId: {}",
                    clientId);

            return result;

        } catch (HttpClientErrorException e) {
            log.error("CDC client error for clientId {}: {} — {}",
                    clientId, e.getStatusCode(),
                    e.getResponseBodyAsString());
            throw new RuntimeException(
                    "CDC RCC request failed — client error: "
                            + e.getStatusCode()
            );
        } catch (HttpServerErrorException e) {
            log.error("CDC server error for clientId {}: {}",
                    clientId, e.getStatusCode());
            throw new RuntimeException(
                    "CDC server error: " + e.getStatusCode()
            );
        } catch (ResourceAccessException e) {
            log.error("CDC connection failed for clientId {}: {}",
                    clientId, e.getMessage());
            throw new RuntimeException(
                    "Could not connect to CDC. "
                            + "Check network and CDC URL configuration."
            );
        }
    }

    /**
     * Builds CDC RCC request from real Fineract client data.
     * Maps ClientData fields to CirculoDeCreditoRCCRequest format.
     */
    private CirculoDeCreditoRCCRequest buildRequest(ClientData client) {

        // Build street address string from list
        String streetAddress = "";
        List<String> addressLines = client.getStreetAddress();
        if (addressLines != null && !addressLines.isEmpty()) {
            streetAddress = String.join(" ", addressLines);
        }

        return CirculoDeCreditoRCCRequest.builder()
                // Person fields from Fineract
                .primerNombre(nvl(client.getFirstName()))
                .apellidoPaterno(nvl(client.getLastName()))
                .fechaNacimiento(formatDate(client.getDateOfBirth()))
                // Address fields from Fineract
                .domicilio(CirculoDeCreditoRCCRequest.Domicilio.builder()
                        .direccion(nvl(streetAddress))
                        .ciudad(nvl(client.getCity()))
                        .estado(nvl(client.getStateProvince()))
                        .codigoPostal(nvl(client.getPostalCode()))
                        .municipio(nvl(client.getTownVillage()))
                        .build())
                .build();
    }

    /**
     * Sandbox test endpoint — uses hardcoded test data
     * as provided by Circulo de Credito for sandbox testing.
     */
    public ResponseEntity<String> testRCCSandboxEndpoint(
            Long creditBureauId) throws Exception {

        String url = baseUrl + "sandbox/v1/rcc";

        // Hardcoded CDC sandbox test data (official CDC test values)
        CirculoDeCreditoRCCRequest request = CirculoDeCreditoRCCRequest
                .builder()
                .primerNombre("JUAN")
                .apellidoPaterno("PRUEBA")
                .apellidoMaterno("CUATRO")
                .fechaNacimiento("1980-01-04")
                .rfc("PUAC800107")
                .domicilio(CirculoDeCreditoRCCRequest.Domicilio.builder()
                        .direccion("INSURGENTES SUR 1007")
                        .colonia("INSURGENTES SUR")
                        .municipio("MEXICO CITY")
                        .ciudad("MEXICO CITY")
                        .estado("DF")
                        .codigoPostal("11230")
                        .build())
                .build();

        Map<String, String> keys = creditBureauRegistrationReadService
                .getRegistrationParamMap(creditBureauId);
        String apiKey = keys.get("x-api-key");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<CirculoDeCreditoRCCRequest> entity =
                new HttpEntity<>(request, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );
            log.info("CDC sandbox status: {}", response.getStatusCode());
            log.debug("CDC sandbox body: {}", response.getBody());
            return response;

        } catch (HttpClientErrorException e) {
            log.error("CDC sandbox error: {} — {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "CDC sandbox failed: " + e.getStatusCode()
                            + " — " + e.getResponseBodyAsString()
            );
        } catch (ResourceAccessException e) {
            log.error("CDC sandbox connection failed: {}",
                    e.getMessage());
            throw new RuntimeException(
                    "Could not connect to CDC sandbox."
            );
        }
    }

    // Helper methods
    private String nvl(String value) {
        return value != null ? value : "";
    }

    private String formatDate(Object dateOfBirth) {
        if (dateOfBirth == null) return "";
        return dateOfBirth.toString();
    }
}