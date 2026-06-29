package org.mifos.creditbureau.cb_ild.client;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.exception.CdcNotConfiguredException;
import org.mifos.creditbureau.cb_ild.exception.CdcServerException;
import org.mifos.creditbureau.cb_ild.exception.CdcTimeoutException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

/**
 * HTTP client for mifos-x-credit-bureau-plugin (MX-276).
 *
 * Calls POST /circulo-de-credito/rcc/{clientId}?creditBureauId={id}
 * on the plugin running at plugin.base-url (default: localhost:8081).
 *
 * The plugin handles:
 *   - Fetching client RFC from Fineract /identifiers
 *   - Building and signing the CDC RCC request (ECDSA secp384r1)
 *   - Calling CDC sandbox: POST /sandbox/v1/rcc
 *   - Mapping CirculoDeCreditoResponse -> CBCreditReportData
 *
 * CB-ILD only calls this one endpoint and maps the response
 * to BureauResponseEntity via CdcScorePullServiceImpl.
 *
 * Auth: Basic auth via pluginRestTemplate bean (tester:tempPassword123).
 * Timeouts: 10s connect, 30s read (configured in RestTemplateConfig).
 *
 * Security:
 *   Never logs RFC, FICO score, or raw CDC response.
 *   Only logs clientId and operation names.
 */
@Slf4j
@Component
public class CdcPluginClient {

    private final RestTemplate pluginRestTemplate;
    private final String pluginBaseUrl;
    private final int creditBureauId;

    public CdcPluginClient(
            @Qualifier("pluginRestTemplate") RestTemplate pluginRestTemplate,
            @Value("${plugin.base-url}") String pluginBaseUrl,
            @Value("${cbild.plugin.credit-bureau-id:2}") int creditBureauId) {
        this.pluginRestTemplate = pluginRestTemplate;
        this.pluginBaseUrl = pluginBaseUrl.endsWith("/")
                ? pluginBaseUrl.substring(0, pluginBaseUrl.length() - 1)
                : pluginBaseUrl;
        this.creditBureauId = creditBureauId;
        log.info("CdcPluginClient initialized — pluginBaseUrl: {}, creditBureauId: {}",
                pluginBaseUrl, creditBureauId);
    }

    /**
     * Calls the plugin RCC endpoint for a client.
     *
     * POST {pluginBaseUrl}/circulo-de-credito/rcc/{clientId}
     *      ?creditBureauId={creditBureauId}
     *
     * Returns a Map representing CBCreditReportData JSON:
     *   reportId       — CDC folioConsulta reference
     *   person         — name, RFC, DOB (from Fineract + CDC)
     *   creditAccounts — list of tradelines from CDC creditos
     *   inquiries      — list of CDC consultas
     *   scores         — empty list (basic RCC endpoint, no FICO)
     *
     * Throws:
     *   CdcNotConfiguredException — plugin returned 503 (not configured)
     *   CdcServerException        — plugin returned 5xx
     *   CdcTimeoutException       — connection or read timeout
     *
     * @param clientId Fineract client ID
     * @return CBCreditReportData as a Map (deserialized by Jackson)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> fetchCreditReport(Long clientId) {
        String url = UriComponentsBuilder
                .fromHttpUrl(pluginBaseUrl)
                .path("/circulo-de-credito/rcc/{clientId}")
                .queryParam("creditBureauId", creditBureauId)
                .buildAndExpand(clientId)
                .toUriString();

        log.info("Calling plugin RCC endpoint — clientId: {}", clientId);

        try {
            ResponseEntity<Map> response =
                    pluginRestTemplate.postForEntity(url, null, Map.class);

            log.info("Plugin RCC call successful — clientId: {}, status: {}",
                    clientId, response.getStatusCode());

            return response.getBody();

        } catch (HttpClientErrorException e) {
            // 4xx from plugin — client not found in Fineract, bad RFC, etc.
            log.error("Plugin client error — clientId: {}, status: {}",
                    clientId, e.getStatusCode());
            throw new CdcServerException(clientId, e.getStatusCode().value());

        } catch (HttpServerErrorException e) {
            if (e.getStatusCode().value() == 503) {
                log.error("Plugin CDC not configured — clientId: {}", clientId);
                throw new CdcNotConfiguredException();
            }
            log.error("Plugin server error — clientId: {}, status: {}",
                    clientId, e.getStatusCode());
            throw new CdcServerException(clientId,
                    e.getStatusCode().value());

        } catch (ResourceAccessException e) {
            log.error("Plugin timeout — clientId: {}", clientId);
            throw new CdcTimeoutException(clientId,
                    "Plugin connection timeout");
        }
    }
}
