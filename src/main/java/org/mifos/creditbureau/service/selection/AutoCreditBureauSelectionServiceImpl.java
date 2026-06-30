package org.mifos.creditbureau.service.selection;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.selection.CreditBureauSelectionResult;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of automatic credit bureau selection and detection.
 * 
 * Provides automated selection of credit bureaus without manual intervention.
 * Includes caching for performance optimization.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AutoCreditBureauSelectionServiceImpl implements AutoCreditBureauSelectionService {

    private final CreditBureauRepository creditBureauRepository;
    private final CBRegisterParamRepository cbRegisterParamRepository;

    /**
     * Auto-detect a single configured credit bureau.
     * Returns the only configured CB if exactly one exists.
     * 
     * @return Optional containing the CreditBureau
     * @throws IllegalStateException if multiple CBs are configured
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "autoCreditBureauCache", unless = "#result == null")
    public Optional<CreditBureau> autoDetectCreditBureau() {
        log.info("Auto-detecting configured credit bureau...");
        
        List<CreditBureau> allBureaus = creditBureauRepository.findAll();
        
        if (allBureaus.isEmpty()) {
            log.warn("No credit bureaus configured");
            return Optional.empty();
        }
        
        if (allBureaus.size() > 1) {
            log.warn("Multiple credit bureaus configured ({}). Cannot auto-select", allBureaus.size());
            throw new IllegalStateException(
                String.format("Multiple credit bureaus configured (%d). Please specify which one to use.", allBureaus.size())
            );
        }
        
        CreditBureau bureau = allBureaus.get(0);
        log.info("Auto-detected credit bureau: {} (ID: {})", bureau.getCreditBureauName(), bureau.getId());
        return Optional.of(bureau);
    }

    /**
     * Auto-detect credit bureau by organization ID.
     * 
     * @param organizationId the organization ID
     * @return Optional containing the CreditBureau
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<CreditBureau> autoDetectCreditBureauByOrganization(Long organizationId) {
        log.info("Auto-detecting credit bureau for organization: {}", organizationId);
        
        // If organization ID is provided, filter by it when implemented
        // For now, return the first available CB
        return autoDetectCreditBureau();
    }

    /**
     * Auto-select credit bureau for a specific client.
     * 
     * @param clientId the client ID
     * @return CreditBureauSelectionResult
     */
    @Override
    @Transactional(readOnly = true)
    public CreditBureauSelectionResult autoSelectForClient(Long clientId) {
        log.info("Auto-selecting credit bureau for client: {}", clientId);
        
        try {
            Optional<CreditBureau> bureau = autoDetectCreditBureau();
            
            if (bureau.isEmpty()) {
                return CreditBureauSelectionResult.builder()
                    .selected(false)
                    .status("NOT_CONFIGURED")
                    .selectionReason("No credit bureau configured")
                    .errorMessage("Cannot select credit bureau: none configured")
                    .build();
            }
            
            CreditBureau selectedBureau = bureau.get();
            
            // Validate the selected bureau is ready to use
            if (!isCreditBureauReadyToUse(selectedBureau.getId())) {
                return CreditBureauSelectionResult.builder()
                    .creditBureauId(selectedBureau.getId())
                    .creditBureauName(selectedBureau.getCreditBureauName())
                    .selected(false)
                    .status("INVALID_CREDENTIALS")
                    .selectionReason("Credit bureau configured but not ready")
                    .errorMessage("Credit bureau credentials not properly configured")
                    .build();
            }
            
            log.info("Successfully auto-selected credit bureau {} for client {}", 
                selectedBureau.getCreditBureauName(), clientId);
            
            return CreditBureauSelectionResult.builder()
                .creditBureauId(selectedBureau.getId())
                .creditBureauName(selectedBureau.getCreditBureauName())
                .selected(true)
                .status("READY")
                .selectionReason("Auto-selected available configured credit bureau")
                .build();
                
        } catch (Exception e) {
            log.error("Error auto-selecting credit bureau for client {}: {}", clientId, e.getMessage(), e);
            return CreditBureauSelectionResult.builder()
                .selected(false)
                .status("ERROR")
                .errorMessage("Error during auto-selection: " + e.getMessage())
                .build();
        }
    }

    /**
     * Get the primary/default credit bureau.
     * 
     * @return the primary CreditBureau
     * @throws IllegalStateException if no CB is configured
     */
    @Override
    @Transactional(readOnly = true)
    public CreditBureau getPrimaryCreditBureau() {
        log.debug("Retrieving primary credit bureau");
        return autoDetectCreditBureau()
            .orElseThrow(() -> {
                log.error("No credit bureau configured");
                return new IllegalStateException("No credit bureau configured");
            });
    }

    /**
     * Check if a credit bureau is properly configured and ready to use.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if configured and ready
     */
    @Override
    @Transactional(readOnly = true)
    public boolean isCreditBureauReadyToUse(Long creditBureauId) {
        log.debug("Checking if credit bureau {} is ready to use", creditBureauId);
        
        Optional<CBRegisterParams> params = cbRegisterParamRepository.findById(creditBureauId);
        
        if (params.isEmpty()) {
            log.warn("Credit bureau {} has no configuration parameters", creditBureauId);
            return false;
        }
        
        Map<String, String> registrationParams = params.get().getRegistrationParams();
        
        // Check if required parameters are present
        boolean hasRequiredParams = registrationParams != null && 
            !registrationParams.isEmpty() &&
            registrationParams.containsKey("username") &&
            registrationParams.containsKey("x-api-key");
        
        if (!hasRequiredParams) {
            log.warn("Credit bureau {} is missing required configuration parameters", creditBureauId);
        }
        
        return hasRequiredParams;
    }

    /**
     * Validate credit bureau configuration.
     * 
     * @param creditBureauId the credit bureau ID
     * @return CreditBureauValidationResult
     */
    @Override
    @Transactional(readOnly = true)
    public CreditBureauValidationResult validateConfiguration(Long creditBureauId) {
        log.info("Validating configuration for credit bureau: {}", creditBureauId);
        
        Optional<CreditBureau> bureau = creditBureauRepository.findById(creditBureauId);
        
        if (bureau.isEmpty()) {
            log.warn("Credit bureau {} not found", creditBureauId);
            return CreditBureauValidationResult.builder()
                .creditBureauId(creditBureauId)
                .isValid(false)
                .errors(List.of("Credit bureau not found"))
                .hasValidCredentials(false)
                .hasActiveConnection(false)
                .build();
        }
        
        Optional<CBRegisterParams> params = cbRegisterParamRepository.findById(creditBureauId);
        java.util.List<String> errors = new java.util.ArrayList<>();
        java.util.List<String> warnings = new java.util.ArrayList<>();
        
        if (params.isEmpty()) {
            errors.add("No configuration parameters found");
        } else {
            Map<String, String> registrationParams = params.get().getRegistrationParams();
            
            // Check required fields
            if (registrationParams == null || registrationParams.isEmpty()) {
                errors.add("Configuration parameters are empty");
            } else {
                if (!registrationParams.containsKey("username")) {
                    errors.add("Missing required parameter: username");
                }
                if (!registrationParams.containsKey("x-api-key")) {
                    errors.add("Missing required parameter: x-api-key");
                }
                
                // Certificate is optional but recommended
                if (!registrationParams.containsKey("certificate")) {
                    warnings.add("Certificate parameter not configured (optional)");
                }
            }
        }
        
        boolean isValid = errors.isEmpty();
        log.info("Configuration validation for credit bureau {}: {}", 
            creditBureauId, isValid ? "VALID" : "INVALID");
        
        return CreditBureauValidationResult.builder()
            .creditBureauId(creditBureauId)
            .isValid(isValid)
            .errors(errors)
            .warnings(warnings)
            .hasValidCredentials(isValid)
            .hasActiveConnection(isValid) // TODO: Implement actual connectivity test
            .lastValidatedAt(java.time.LocalDateTime.now().toString())
            .build();
    }

    /**
     * Clear the cache of detected credit bureaus.
     */
    @Override
    @CacheEvict(value = "autoCreditBureauCache", allEntries = true)
    public void clearCache() {
        log.info("Cleared auto credit bureau selection cache");
    }
}
