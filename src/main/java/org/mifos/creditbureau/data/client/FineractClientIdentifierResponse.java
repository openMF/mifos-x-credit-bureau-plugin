package org.mifos.creditbureau.data.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Maps GET /clients/{id}/identifiers response.
 *
 * documentType.value = "NATIONAL_ID" → documentKey = RFC
 * RFC is CDC primary matching key — CDC-001 without it.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FineractClientIdentifierResponse implements Serializable {

    private DocumentType documentType;
    private String documentKey;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DocumentType {
        private String value;
    }

    public boolean isNationalId() {
        return documentType != null
                && "NATIONAL_ID".equals(documentType.getValue())
                && documentKey != null
                && !documentKey.isBlank();
    }
}
