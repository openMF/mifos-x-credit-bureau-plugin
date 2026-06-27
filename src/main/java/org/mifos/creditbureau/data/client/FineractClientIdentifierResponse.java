package org.mifos.creditbureau.data.client;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.io.Serializable;
/**
 * Maps GET /clients/{id}/identifiers response from Apache Fineract.
 *
 * MX-276 fix: Fineract returns documentType.name (not documentType.value).
 * isNationalId() now checks both fields for Fineract version compatibility.
 * Also accepts "Any Other Id Type" for mifos-bank-1 sandbox where
 * no NATIONAL_ID document type exists.
 *
 * RFC is the primary matching key for Circulo de Credito.
 */
@Getter
@NoArgsConstructor(force = true)
@AllArgsConstructor
public class FineractClientIdentifierResponse implements Serializable {
    private final DocumentType documentType;
    private final String documentKey;
    /**
     * Nested class representing the document type from Fineract identifiers API.
     * Fineract 1.x returns documentType.name, older versions return .value.
     * Both fields mapped for compatibility.
     */
    @Getter
    @NoArgsConstructor(force = true)
    @AllArgsConstructor
    public static class DocumentType {
        private final String value;
        private final String name;
    }
    /**
     * Returns true if this identifier is a Mexican RFC (National ID).
     *
     * Accepts the following document type names:
     *   - "NATIONAL_ID" (standard — preferred)
     *   - "Any Other Id Type" (mifos-bank-1 sandbox fallback)
     *   - "Id" (generic ID type fallback)
     *
     * Checks documentType.name first (Fineract 1.x),
     * then documentType.value (older Fineract versions).
     *
     * @return true if this identifier contains a valid RFC value
     */
    public boolean isNationalId() {
        if (documentKey == null || documentKey.isBlank()) return false;
        // Unknown type — do not assume RFC, return false to avoid wrong data to CDC
        if (documentType == null) return false;
        String typeName = documentType.getName() != null
                ? documentType.getName()
                : documentType.getValue();
        // Unknown type name — do not assume RFC
        if (typeName == null) return false;
        return "NATIONAL_ID".equalsIgnoreCase(typeName)
                || "Any Other Id Type".equalsIgnoreCase(typeName)
                || "Id".equalsIgnoreCase(typeName);
    }
}
