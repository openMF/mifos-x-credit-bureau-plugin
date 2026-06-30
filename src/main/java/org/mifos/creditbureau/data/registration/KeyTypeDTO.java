package org.mifos.creditbureau.data.registration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Generic key configuration DTO supporting various authentication methods.
 * Supports asymmetric (signature), symmetric (API key), OAuth, and mTLS authentication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KeyTypeDTO {

    /**
     * Type of key (e.g., API_KEY, SIGNATURE, OAUTH2, MTLS).
     * Uppercase snake case format.
     */
    @NotBlank(message = "keyType must not be blank")
    @Pattern(
        regexp = "^[A-Z_]+$",
        message = "keyType must be uppercase snake case (e.g., API_KEY, SIGNATURE)"
    )
    @Size(max = 64, message = "keyType must not exceed 64 characters")
    private String keyType;

    /**
     * Format of the key (PRIVATE or PUBLIC).
     * Used to distinguish between asymmetric key pairs.
     */
    @NotBlank(message = "format must not be blank")
    @Pattern(
        regexp = "^(PRIVATE|PUBLIC)$",
        message = "format must be either PRIVATE or PUBLIC"
    )
    private String format;

    /**
     * Encrypted or raw key value.
     * Can be a base64-encoded certificate, API key string, or encrypted credential.
     */
    @NotBlank(message = "value must not be blank")
    @Size(max = 8192, message = "value must not exceed 8192 characters")
    private String value;
}
