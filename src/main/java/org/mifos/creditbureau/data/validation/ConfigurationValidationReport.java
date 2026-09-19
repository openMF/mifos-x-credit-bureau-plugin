package org.mifos.creditbureau.data.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * Report of credit bureau configuration validation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigurationValidationReport {
    private Long creditBureauId;
    private String creditBureauName;
    private boolean isValid;
    private boolean hasRequiredFields;
    private boolean credentialsValid;
    private boolean connectionEstablished;
    private List<String> errors;
    private List<String> warnings;
    private String validatedAt;
    private String lastValidationStatus; // SUCCESS, FAILED, PENDING
    private long validationDurationMs;
}
