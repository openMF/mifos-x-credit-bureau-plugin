package org.mifos.creditbureau.service.selection;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Result of credit bureau configuration validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditBureauValidationResult {
    private Long creditBureauId;
    private boolean isValid;
    private List<String> errors;
    private List<String> warnings;
    private String lastValidatedAt;
    private boolean hasValidCredentials;
    private boolean hasActiveConnection;
}
