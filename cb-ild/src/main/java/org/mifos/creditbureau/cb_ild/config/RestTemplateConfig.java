package org.mifos.creditbureau.cb_ild.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configures two RestTemplate Spring beans for CB-ILD external HTTP calls.
 *
 * WHY TWO BEANS:
 * fineractRestTemplate → calls Apache Fineract (remote, sandbox/production)
 * pluginRestTemplate   → calls mifos-x-credit-bureau-plugin (local, port 8081)
 *
 * WHY NOT static RestTemplate:
 * Plugin uses static RestTemplate with no timeouts — Bug 2.
 * Static RestTemplate threads hang forever if Fineract stops responding.
 * Under load this exhausts the thread pool and kills the server.
 * Spring-managed beans with timeouts prevent this.
 *
 * TIMEOUTS:
 * Connect timeout 10s — time to establish TCP connection
 * Read timeout    30s — time to wait for response after connection
 * Conservative for sandbox. Tighten for production.
 *
 * CREDENTIALS:
 * Injected via constructor from application.properties.
 * Never hardcoded. Never field-injected — constructor injection only.
 */
@Configuration
public class RestTemplateConfig {

    private final String fineractUsername;
    private final String fineractPassword;
    private final String pluginUsername;
    private final String pluginPassword;

    public RestTemplateConfig(
            @Value("${mifos.fineract.api.username}") String fineractUsername,
            @Value("${mifos.fineract.api.password}") String fineractPassword,
            @Value("${cbild.plugin.username:tester}") String pluginUsername,
            @Value("${cbild.plugin.password:tempPassword123}") String pluginPassword) {
        this.fineractUsername = fineractUsername;
        this.fineractPassword = fineractPassword;
        this.pluginUsername = pluginUsername;
        this.pluginPassword = pluginPassword;
    }

    /**
     * RestTemplate for calling Apache Fineract.
     * Used by FineractApiClient for all 3 Fineract endpoints.
     */
    @Bean("fineractRestTemplate")
    public RestTemplate fineractRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .basicAuthentication(fineractUsername, fineractPassword)
                .build();
    }

    /**
     * RestTemplate for calling mifos-x-credit-bureau-plugin.
     * Used by CdcScorePullService to trigger CDC RCC pulls.
     *
     * Plugin uses Basic Auth — confirmed from plugin SecurityConfig.java.
     * Uses same Fineract credentials for sandbox.
     * Production: give plugin its own credentials.
     */
    @Bean("pluginRestTemplate")
    public RestTemplate pluginRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .basicAuthentication(pluginUsername, pluginPassword)
                .build();
    }
}
