package org.mifos.creditbureau.service.selection;

import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.data.selection.CreditBureauSelectionResult;

import java.util.Optional;

/**
 * Service for automatic credit bureau selection and detection.
 * 
 * Handles:
 * - Auto-detecting configured credit bureaus for an organization
 * - Auto-selecting the appropriate CB based on rules/criteria
 * - Caching results for performance
 */
public interface AutoCreditBureauSelectionService {

    /**
     * Auto-detect a single configured credit bureau for the organization.
     * 
     * @return Optional containing the detected CreditBureau, or empty if none/multiple found
     * @throws IllegalStateException if multiple credit bureaus are configured
     */
    Optional<CreditBureau> autoDetectCreditBureau();

    /**
     * Auto-detect credit bureau by organization ID.
     * 
     * @param organizationId the organization ID
     * @return Optional containing the detected CreditBureau
     */
    Optional<CreditBureau> autoDetectCreditBureauByOrganization(Long organizationId);

    /**
     * Auto-select credit bureau for a specific client based on rules.
     * 
     * @param clientId the client ID
     * @return CreditBureauSelectionResult containing selection details
     */
    CreditBureauSelectionResult autoSelectForClient(Long clientId);

    /**
     * Get the primary/default credit bureau.
     * 
     * @return the primary CreditBureau
     * @throws IllegalStateException if no credit bureau is configured
     */
    CreditBureau getPrimaryCreditBureau();

    /**
     * Check if a credit bureau is properly configured and ready to use.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if configured and ready, false otherwise
     */
    boolean isCreditBureauReadyToUse(Long creditBureauId);

    /**
     * Validate credit bureau configuration.
     * 
     * @param creditBureauId the credit bureau ID
     * @return validation result with detailed messages
     */
    CreditBureauValidationResult validateConfiguration(Long creditBureauId);

    /**
     * Clear the cache of detected credit bureaus.
     * Useful for testing or after configuration changes.
     */
    void clearCache();
}
