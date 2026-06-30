package org.mifos.creditbureau.cb_ild.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cbIldOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CB-ILD — Credit Bureau Information Lifecycle Dashboard")
                        .description("""
                                ## What is CB-ILD?
                                Bridges Apache Fineract MFIs with Circulo de Credito (CDC),
                                Mexico's largest credit bureau. MSOC 2026 — Mifos Initiative.

                                ## 3 CDC Trigger Points
                                - **Trigger 1** — Pre-approval KYC (`GET /api/clients/{id}/bureau-readiness`)
                                - **Trigger 2** — Post-approval reporting (`POST /api/submissions/report-approval`)
                                - **Trigger 3** — Screening event (`POST /api/submissions/report-screening`)

                                ## 3 User Roles
                                | Role | Username | Password | Access |
                                |---|---|---|---|
                                | KYC Officer | `kyc_officer` | `password` | Bureau readiness, screening, disputes |
                                | Credit Analyst | `credit_analyst` | `password` | Submissions, bureau response, disputes |
                                | Compliance | `compliance` | `password` | Audit trail + all read endpoints |

                                ## How to authenticate
                                Click the **Authorize** button (top right), enter username and password, click Authorize, then Close.

                                ## Compliance
                                - **LRSIC** — Mexican law, 72-month data retention
                                - **CONDUSEF** — Mexico financial regulator
                                - RFC (Mexican tax ID) is never logged anywhere
                                - Every CDC interaction is auto-audited via Spring AOP
                                """)
                        .version("2.0.0 — MSOC 2026")
                        .contact(new Contact()
                                .name("Satyam Mishra — MSOC 2026")
                                .url("https://github.com/saksham869/cb-ild"))
                        .license(new License()
                                .name("Mozilla Public License 2.0")
                                .url("https://www.mozilla.org/en-US/MPL/2.0/")))
                .addSecurityItem(new SecurityRequirement().addList("basicAuth"))
                .components(new Components()
                        .addSecuritySchemes("basicAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description(
                                    "Basic Auth. Use: kyc_officer/password OR credit_analyst/password OR compliance/password"
                                )));
    }
}
