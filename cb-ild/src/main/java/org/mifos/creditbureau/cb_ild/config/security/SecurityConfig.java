package org.mifos.creditbureau.cb_ild.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration — MX-276 (Phase 2).
 *
 * Phase 1 was permitAll() — no auth enforcement.
 * MX-276 activates Basic Auth + RBAC via @EnableMethodSecurity
 * on CbIldApplication.
 *
 * Auth: HTTP Basic — same pattern as mifos-x-credit-bureau-plugin.
 * Fineract sandbox uses Basic auth (Victor confirmed no OAuth2).
 * JWT can replace Basic auth in a later phase without changing any
 * @PreAuthorize rules — role names stay identical.
 *
 * RBAC roles (3 roles, matching all @PreAuthorize annotations):
 *   KYC_OFFICER    — bureau readiness + disputes
 *   CREDIT_ANALYST — submissions + disputes
 *   COMPLIANCE     — everything above
 *
 * Session: STATELESS — no HttpSession created or used.
 * CSRF: disabled — REST API, no browser form submissions.
 *
 * Actuator + Swagger: permitted without auth for monitoring.
 * All other endpoints: require authentication.
 * Role enforcement: done by @PreAuthorize (activated by
 * @EnableMethodSecurity on CbIldApplication).
 *
 * Passwords: {noop} prefix = plain text, no encoding.
 * Production: replace with BCrypt + secrets manager.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Security filter chain — MX-276.
     *
     * permitAll paths:
     *   /actuator/** — health checks, metrics
     *   /v3/api-docs/** — Swagger/OpenAPI schema
     *   /swagger-ui/** — Swagger UI
     *
     * All other paths require authentication.
     * Role-level enforcement handled by @PreAuthorize on controllers.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html")
                .permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    /**
     * In-memory users — 3 roles matching RBAC table.
     *
     * KYC_OFFICER:
     *   GET  /api/clients/{id}/bureau-readiness
     *   POST /api/disputes
     *   PUT  /api/disputes/{id}/status
     *   GET  /api/disputes/{id}
     *
     * CREDIT_ANALYST:
     *   POST /api/submissions/run
     *   GET  /api/submissions/history
     *   POST /api/disputes
     *   PUT  /api/disputes/{id}/status
     *   GET  /api/disputes/{id}
     *
     * COMPLIANCE: all of the above.
     *
     * Production: replace with database-backed UserDetailsService
     * or LDAP/OAuth2 integration.
     */
    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails kycOfficer = User.withUsername("kyc_officer")
                .password("{noop}password")
                .roles("KYC_OFFICER")
                .build();

        UserDetails creditAnalyst = User.withUsername("credit_analyst")
                .password("{noop}password")
                .roles("CREDIT_ANALYST")
                .build();

        UserDetails compliance = User.withUsername("compliance")
                .password("{noop}password")
                .roles("COMPLIANCE")
                .build();

        return new InMemoryUserDetailsManager(
                kycOfficer, creditAnalyst, compliance);
    }
}
