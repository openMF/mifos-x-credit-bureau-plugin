package org.mifos.creditbureau.cb_ild.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Maps one identifier entry from Fineract GET /clients/{id}/identifiers.
 *
 * Real Fineract sandbox response:
 * [] — empty array, RFC never stored (Bug 1 this file helps fix)
 *
 * Expected response when RFC is present:
 * [{"documentType":{"value":"NATIONAL_ID"},"documentKey":"ABC123456"}]
 *
 * IMPORTANT: documentType is a nested JSON object, not a plain string.
 * Jackson must deserialize it as a nested record — that is why
 * DocumentType is defined as an inner record here.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FineractIdentifierDTO(
        DocumentType documentType,
        String documentKey
) {

    /**
     * Fineract returns documentType as a nested object:
     * {"value": "NATIONAL_ID"}
     * Not as a plain string "NATIONAL_ID".
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DocumentType(String value) {}

    /**
     * Returns true if this identifier is a valid National ID with a
     * non-blank documentKey (RFC value).
     *
     * Checks both:
     * 1. documentType.value == "NATIONAL_ID"
     * 2. documentKey is not null and not blank
     *
     * An empty string documentKey would cause CDC-001 just like null.
     * RFC is CDC primary matching key — must be present and non-empty.
     */
    public boolean isNationalId() {
        return documentType != null
                && "NATIONAL_ID".equals(documentType.value())
                && documentKey != null
                && !documentKey.isBlank();
    }
}
