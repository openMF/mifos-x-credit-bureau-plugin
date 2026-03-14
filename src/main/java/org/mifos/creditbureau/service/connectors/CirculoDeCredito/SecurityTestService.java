package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Service
public class SecurityTestService {

    @Value("${mifos.circulodecredito.base.url}")
    private String circuloDeCreditoBaseUrl;

    @Value("${mifos.circulodecredito.mock.enabled:false}")
    private boolean mockEnabled;

    private final SignatureService signatureService;
    private final RestTemplate restTemplate;

    public SecurityTestService(
            SignatureService signatureService,
            RestTemplateBuilder restTemplateBuilder) {
        this.signatureService = signatureService;
        // FIX: Added timeouts — no longer hangs forever
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }

    public String testSecurityEndpoint(Long creditBureauId) throws Exception {

        // Validate input
        if (creditBureauId == null) {
            throw new IllegalArgumentException(
                    "Credit bureau ID must not be null"
            );
        }

        // FIX: Mock mode for local development — no real CDC call
        if (mockEnabled) {
            log.info("Mock mode enabled — skipping real CDC security test");
            return "{\"status\":\"mock_success\","
                    + "\"message\":\"Mock security test passed\"}";
        }

        String url = circuloDeCreditoBaseUrl + "/v1/securitytest";
        String requestBody = "{\"attribute\":\"Hello World!\"}";

        log.info("Testing CDC security endpoint for bureau ID: {}",
                creditBureauId);

        try {
            Map<String, String> headersMap = signatureService
                    .buildHeaders(creditBureauId, requestBody);

            HttpHeaders headers = new HttpHeaders();
            headersMap.forEach(headers::add);

            HttpEntity<String> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class
            );

            // FIX: Replaced System.out.println with proper logging
            log.info("CDC security test status: {}",
                    response.getStatusCode());
            log.debug("CDC security test response body: {}",
                    response.getBody());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 4xx errors — bad request, unauthorized, etc.
            log.error("CDC client error during security test: {} — {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "CDC security test failed — client error: "
                            + e.getStatusCode() + " — "
                            + e.getResponseBodyAsString()
            );
        } catch (HttpServerErrorException e) {
            // 5xx errors from CDC side
            log.error("CDC server error during security test: {} — {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException(
                    "CDC security test failed — server error: "
                            + e.getStatusCode()
            );
        } catch (ResourceAccessException e) {
            // Network timeout or connection refused
            log.error("CDC connection failed during security test: {}",
                    e.getMessage());
            throw new RuntimeException(
                    "CDC security test failed — could not connect to CDC. "
                            + "Check network and CDC URL configuration."
            );
        }
    }
}