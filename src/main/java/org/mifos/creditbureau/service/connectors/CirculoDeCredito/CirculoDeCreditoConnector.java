package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.CBCreditReportData;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRCCRequest;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoResponse;
import org.mifos.creditbureau.mappers.CirculoDeCreditoResponseToCBCreditReportDataMapper;
import org.mifos.creditbureau.mappers.ClientDatatoCirculoDeCreditoRccRequest;
import org.mifos.creditbureau.service.connectors.CreditBureauConnector;
import org.springframework.beans.factory.annotation.Value;
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

import java.time.Instant;
import java.util.Map;

/**
 * Circulo de Credito connector implementation.
 * <p>
 * Orchestrates existing services to fetch credit reports:
 * <ol>
 *   <li>Maps {@link ClientData} → {@link CirculoDeCreditoRCCRequest} (existing mapper)</li>
 *   <li>Serializes to JSON and signs with ECDSA (existing {@link SignatureService})</li>
 *   <li>POSTs to CDC production endpoint</li>
 *   <li>Maps response → {@link CBCreditReportData} (existing response mapper)</li>
 * </ol>
 */
@Service
@Slf4j
public class CirculoDeCreditoConnector implements CreditBureauConnector {

    private static final String BUREAU_TYPE = "CIRCULO_DE_CREDITO";

    private final SignatureService signatureService;
    private final ClientDatatoCirculoDeCreditoRccRequest requestMapper;
    private final CirculoDeCreditoResponseToCBCreditReportDataMapper responseMapper;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${mifos.circulodecredito.base.url}")
    private String baseUrl;

    public CirculoDeCreditoConnector(
            SignatureService signatureService,
            ClientDatatoCirculoDeCreditoRccRequest requestMapper,
            CirculoDeCreditoResponseToCBCreditReportDataMapper responseMapper,
            ObjectMapper objectMapper) {
        this.signatureService = signatureService;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    CirculoDeCreditoConnector(
            SignatureService signatureService,
            ClientDatatoCirculoDeCreditoRccRequest requestMapper,
            CirculoDeCreditoResponseToCBCreditReportDataMapper responseMapper,
            ObjectMapper objectMapper,
            RestTemplate restTemplate,
            String baseUrl) {
        this.signatureService = signatureService;
        this.requestMapper = requestMapper;
        this.responseMapper = responseMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public String getBureauType() {
        return BUREAU_TYPE;
    }

    @Override
    public CreditReportResult fetchCreditReport(Long creditBureauId, ClientData clientData) {
        log.info("Fetching credit report from Circulo de Credito for bureau={}", creditBureauId);

        try {
            CirculoDeCreditoRCCRequest request = requestMapper
                    .toCirculoDeCreditoRccRequest(clientData);

            String requestBody = objectMapper.writeValueAsString(request);
            log.debug("CDC request body: {}", requestBody);

            Map<String, String> headersMap = signatureService
                    .buildHeaders(creditBureauId, requestBody);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headersMap.forEach(headers::set);

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String url = baseUrl + "v1/rcc";
            log.info("Sending RCC request to {}", url);

            ResponseEntity<String> response = restTemplate
                    .exchange(url, HttpMethod.POST, entity, String.class);

            CirculoDeCreditoResponse cdcResponse = objectMapper
                    .readValue(response.getBody(), CirculoDeCreditoResponse.class);

            CBCreditReportData report = responseMapper.toCBCreditReportData(cdcResponse);

            log.info("Successfully fetched credit report from Circulo de Credito");
            return CreditReportResult.builder()
                    .success(true)
                    .report(report)
                    .bureauType(BUREAU_TYPE)
                    .rawResponse(response.getBody())
                    .fetchedAt(Instant.now())
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("CDC returned client error {}: {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(
                    "Credit bureau rejected request (HTTP " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(), e);
        } catch (HttpServerErrorException e) {
            log.error("CDC returned server error {}: {}", e.getStatusCode(), e.getMessage());
            throw new RuntimeException(
                    "Credit bureau unavailable (HTTP " + e.getStatusCode().value() + "): "
                            + e.getResponseBodyAsString(), e);
        } catch (ResourceAccessException e) {
            log.error("CDC connection failed: {}", e.getMessage());
            throw new RuntimeException(
                    "Unable to reach credit bureau: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error fetching credit report: {}", e.getMessage(), e);
            throw new RuntimeException(
                    "Failed to fetch credit report: " + e.getMessage(), e);
        }
    }

    @Override
    public ConnectionTestResult testConnection(Long creditBureauId) {
        log.info("Testing connection to Circulo de Credito for bureau={}", creditBureauId);

        try {
            String testBody = "{\"attribute\":\"Hello World!\"}";

            Map<String, String> headersMap = signatureService
                    .buildHeaders(creditBureauId, testBody);
            HttpHeaders headers = new HttpHeaders();
            headersMap.forEach(headers::set);

            HttpEntity<String> entity = new HttpEntity<>(testBody, headers);
            String url = baseUrl + "v1/securitytest";

            ResponseEntity<String> response = restTemplate
                    .exchange(url, HttpMethod.POST, entity, String.class);

            log.info("CDC security test returned status {}", response.getStatusCode());
            return ConnectionTestResult.builder()
                    .success(response.getStatusCode().is2xxSuccessful())
                    .bureauType(BUREAU_TYPE)
                    .message(response.getBody())
                    .httpStatusCode(response.getStatusCode().value())
                    .build();

        } catch (Exception e) {
            log.error("CDC connection test failed: {}", e.getMessage());
            return ConnectionTestResult.builder()
                    .success(false)
                    .bureauType(BUREAU_TYPE)
                    .message("Connection test failed: " + e.getMessage())
                    .httpStatusCode(0)
                    .build();
        }
    }
}
