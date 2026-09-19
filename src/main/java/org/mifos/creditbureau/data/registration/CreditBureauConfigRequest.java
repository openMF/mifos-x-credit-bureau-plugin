package org.mifos.creditbureau.data.registration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for credit bureau configuration.
 * Designed to be extensible for various credit bureaus with different authentication methods.
 * 
 * Example: CDC requires 2 keys (asymmetric signing), while others may require 1 or 4 keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauConfigRequest {

    /**
     * Organization credit bureau ID (path parameter validation).
     */
    @NotNull(message = "organisationCreditBureauId must not be null")
    private Long organisationCreditBureauId;

    /**
     * Type of credit bureau (e.g., "CDC", "Equifax", "CreditFair").
     * Helps identify the bureau for proper key handling.
     */
    @NotBlank(message = "bureauType must not be blank")
    @Size(max = 100, message = "bureauType must not exceed 100 characters")
    private String bureauType;

    /**
     * Flexible list of keys for the credit bureau.
     * Each key can represent an API key, certificate, or other credential type.
     * 
     * Examples:
     * - Single API_KEY for symmetric authentication
     * - Two keys (PRIVATE, PUBLIC) for asymmetric signing
     * - Multiple keys for multi-factor or complex authentication
     */
    @NotNull(message = "keys list must not be null")
    @Size(min = 1, message = "at least one key configuration is required")
    private List<@Valid KeyTypeDTO> keys;
}

