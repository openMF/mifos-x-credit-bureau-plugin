package org.mifos.creditbureau.data.registration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for credit bureau configuration endpoint.
 * Masks sensitive values to prevent exposure of encrypted credentials.
 * Returns configured key types instead of actual values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauConfigResponse {

    /**
     * Internal ID of the configuration record.
     */
    @JsonProperty("id")
    private Long id;

    /**
     * Organisation credit bureau ID for reference.
     */
    @JsonProperty("organisationCreditBureauId")
    private Long organisationCreditBureauId;

    /**
     * Type of credit bureau (e.g., "CDC", "Equifax").
     */
    @JsonProperty("bureauType")
    private String bureauType;

    /**
     * List of configured key types without exposing actual values.
     * Format: "KEY_TYPE:FORMAT" (e.g., "API_KEY:PRIVATE", "SIGNATURE:PUBLIC")
     */
    @JsonProperty("configuredKeyTypes")
    private List<String> configuredKeyTypes;

    /**
     * Success message confirming configuration update.
     */
    @JsonProperty("message")
    private String message;
}
