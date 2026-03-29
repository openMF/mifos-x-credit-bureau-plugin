package org.mifos.creditbureau.data.selection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Result of credit bureau selection process.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauSelectionResult {
    private Long creditBureauId;
    private String creditBureauName;
    private boolean selected;
    private String selectionReason;
    private String status; // READY, NOT_CONFIGURED, INVALID_CREDENTIALS
    private String errorMessage;
}
