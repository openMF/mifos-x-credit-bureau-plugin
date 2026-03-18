package org.mifos.creditbureau.data.registration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Response DTO for credit bureau configuration endpoint.
 * Masks sensitive values to prevent exposure of encrypted credentials.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauConfigResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("organisationCreditBureauId")
    private Long organisationCreditBureauId;

    /**
     * Returns the set of configuration keys that were updated.
     * Does NOT return the encrypted values themselves.
     */
    @JsonProperty("configuredKeys")
    private Set<String> configuredKeys;

    @JsonProperty("message")
    private String message;
}
