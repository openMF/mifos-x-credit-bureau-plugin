package org.mifos.creditbureau.service.validation;

import org.mifos.creditbureau.data.validation.ConfigurationValidationReport;

/**
 * Service for automatic credit bureau configuration validation.
 * 
 * Handles:
 * - Validating CB connectivity
 * - Checking credential validity
 * - Performing health checks on configuration
 */
public interface AutoConfigurationValidationService {

    /**
     * Validate credit bureau configuration on application startup.
     * 
     * @param creditBureauId the credit bureau ID
     * @return validation report with detailed status
     */
    ConfigurationValidationReport validateOnStartup(Long creditBureauId);

    /**
     * Perform a full configuration health check.
     * 
     * @param creditBureauId the credit bureau ID
     * @return detailed health check report
     */
    ConfigurationValidationReport performHealthCheck(Long creditBureauId);

    /**
     * Test connection to the credit bureau API.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if connection is successful
     */
    boolean testConnection(Long creditBureauId);

    /**
     * Validate stored credentials.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if credentials are valid and can be decrypted
     */
    boolean validateCredentials(Long creditBureauId);

    /**
     * Check if all required configuration fields are present.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if all required fields are configured
     */
    boolean hasRequiredFields(Long creditBureauId);

    /**
     * Get the last validation timestamp for a credit bureau.
     * 
     * @param creditBureauId the credit bureau ID
     * @return last validation timestamp, or null if never validated
     */
    String getLastValidationTime(Long creditBureauId);

    /**
     * Mark a credit bureau as validated (successful configuration).
     * 
     * @param creditBureauId the credit bureau ID
     */
    void markAsValidated(Long creditBureauId);

    /**
     * Clear validation cache for a credit bureau.
     * 
     * @param creditBureauId the credit bureau ID
     */
    void clearValidationCache(Long creditBureauId);

    /**
     * Clear all validation caches.
     */
    void clearAllValidationCaches();
}
