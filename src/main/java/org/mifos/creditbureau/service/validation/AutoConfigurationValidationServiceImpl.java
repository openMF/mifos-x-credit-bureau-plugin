package org.mifos.creditbureau.service.validation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.validation.ConfigurationValidationReport;
import org.mifos.creditbureau.domain.CBRegisterParams;
import org.mifos.creditbureau.domain.CBRegisterParamRepository;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mifos.creditbureau.service.EncryptionService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of automatic configuration validation service.
 * 
 * Validates credit bureau configuration and connectivity automatically
 * without manual intervention. Includes caching and detailed reporting.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AutoConfigurationValidationServiceImpl implements AutoConfigurationValidationService {

    private final CreditBureauRepository creditBureauRepository;
    private final CBRegisterParamRepository cbRegisterParamRepository;
    private final EncryptionService encryptionService;

    /**
     * Validate credit bureau configuration on startup.
     * 
     * @param creditBureauId the credit bureau ID
     * @return validation report
     */
    @Override
    @Transactional(readOnly = true)
    public ConfigurationValidationReport validateOnStartup(Long creditBureauId) {
        log.info("Starting up validation for credit bureau: {}", creditBureauId);
        long startTime = System.currentTimeMillis();
        
        ConfigurationValidationReport report = performHealthCheck(creditBureauId);
        
        long duration = System.currentTimeMillis() - startTime;
        report.setValidationDurationMs(duration);
        
        if (report.isValid()) {
            log.info("✅ Credit bureau {} validated successfully at startup", creditBureauId);
            markAsValidated(creditBureauId);
        } else {
            log.warn("❌ Credit bureau {} failed validation at startup: {}", 
                creditBureauId, String.join(", ", report.getErrors()));
        }
        
        return report;
    }

    /**
     * Perform a full configuration health check.
     * 
     * @param creditBureauId the credit bureau ID
     * @return detailed health check report
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "configValidationCache", key = "#creditBureauId", 
        unless = "#result == null || !#result.isValid()")
    public ConfigurationValidationReport performHealthCheck(Long creditBureauId) {
        log.debug("Performing health check for credit bureau: {}", creditBureauId);
        
        long startTime = System.currentTimeMillis();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check if CB exists
        Optional<CreditBureau> bureauOpt = creditBureauRepository.findById(creditBureauId);
        if (bureauOpt.isEmpty()) {
            errors.add("Credit bureau not found");
            return buildReport(creditBureauId, null, errors, warnings, 
                false, false, false, System.currentTimeMillis() - startTime);
        }
        
        CreditBureau bureau = bureauOpt.get();
        
        // Check if configuration exists
        Optional<CBRegisterParams> paramsOpt = cbRegisterParamRepository.findById(creditBureauId);
        if (paramsOpt.isEmpty()) {
            errors.add("No configuration parameters found");
            return buildReport(creditBureauId, bureau.getCreditBureauName(), 
                errors, warnings, false, false, false, System.currentTimeMillis() - startTime);
        }
        
        boolean hasRequiredFields = hasRequiredFields(creditBureauId);
        boolean credentialsValid = false;
        boolean connectionOk = false;
        
        // Validate required fields
        if (!hasRequiredFields) {
            errors.add("Missing required configuration fields");
        }
        
        // Try to decrypt and validate credentials
        try {
            credentialsValid = validateCredentials(creditBureauId);
            if (!credentialsValid) {
                errors.add("Credentials could not be validated or decrypted");
            }
        } catch (Exception e) {
            log.error("Error validating credentials for CB {}: {}", creditBureauId, e.getMessage());
            errors.add("Error validating credentials: " + e.getMessage());
        }
        
        // Test connection if credentials are valid
        if (credentialsValid) {
            try {
                connectionOk = testConnection(creditBureauId);
                if (!connectionOk) {
                    warnings.add("Could not establish connection to credit bureau (may be temporary)");
                }
            } catch (Exception e) {
                log.warn("Connection test failed for CB {}: {}", creditBureauId, e.getMessage());
                warnings.add("Connection test failed: " + e.getMessage());
            }
        }
        
        boolean isValid = errors.isEmpty() && hasRequiredFields && credentialsValid;
        
        return buildReport(creditBureauId, bureau.getCreditBureauName(), 
            errors, warnings, isValid, hasRequiredFields, credentialsValid, 
            System.currentTimeMillis() - startTime);
    }

    /**
     * Test connection to the credit bureau API.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if connection is successful
     */
    @Override
    @Transactional(readOnly = true)
    public boolean testConnection(Long creditBureauId) {
        log.debug("Testing connection to credit bureau: {}", creditBureauId);
        
        try {
            // TODO: Implement actual connection test
            // This would call a security test endpoint or ping the CB API
            // For now, return true if credentials exist
            return validateCredentials(creditBureauId);
        } catch (Exception e) {
            log.error("Connection test failed for CB {}: {}", creditBureauId, e.getMessage());
            return false;
        }
    }

    /**
     * Validate stored credentials.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if credentials are valid
     */
    @Override
    @Transactional(readOnly = true)
    public boolean validateCredentials(Long creditBureauId) {
        log.debug("Validating credentials for credit bureau: {}", creditBureauId);
        
        Optional<CBRegisterParams> paramsOpt = cbRegisterParamRepository.findById(creditBureauId);
        
        if (paramsOpt.isEmpty()) {
            return false;
        }
        
        try {
            Map<String, String> params = paramsOpt.get().getRegistrationParams();
            
            if (params == null || params.isEmpty()) {
                return false;
            }
            
            // Try to decrypt username and API key to verify they exist and are decryptable
            String username = params.get("username");
            String apiKey = params.get("x-api-key");
            
            if (username == null || apiKey == null) {
                return false;
            }
            
            // Attempt to decrypt (if encrypted)
            try {
                if (username.contains("$ENCRYPTED$")) {
                    encryptionService.decrypt(username);
                }
                if (apiKey.contains("$ENCRYPTED$")) {
                    encryptionService.decrypt(apiKey);
                }
            } catch (Exception e) {
                log.warn("Failed to decrypt credentials for CB {}: {}", creditBureauId, e.getMessage());
                return false;
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("Error validating credentials for CB {}: {}", creditBureauId, e.getMessage());
            return false;
        }
    }

    /**
     * Check if all required fields are present.
     * 
     * @param creditBureauId the credit bureau ID
     * @return true if all required fields are present
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasRequiredFields(Long creditBureauId) {
        log.debug("Checking required fields for credit bureau: {}", creditBureauId);
        
        Optional<CBRegisterParams> paramsOpt = cbRegisterParamRepository.findById(creditBureauId);
        
        if (paramsOpt.isEmpty()) {
            return false;
        }
        
        Map<String, String> params = paramsOpt.get().getRegistrationParams();
        
        if (params == null || params.isEmpty()) {
            return false;
        }
        
        // Check for required fields
        return params.containsKey("username") && 
               params.containsKey("x-api-key");
    }

    /**
     * Get the last validation timestamp.
     * 
     * @param creditBureauId the credit bureau ID
     * @return last validation time
     */
    @Override
    public String getLastValidationTime(Long creditBureauId) {
        // TODO: Persist validation timestamps in database
        return null;
    }

    /**
     * Mark a credit bureau as validated.
     * 
     * @param creditBureauId the credit bureau ID
     */
    @Override
    public void markAsValidated(Long creditBureauId) {
        log.info("Marking credit bureau {} as validated", creditBureauId);
        // TODO: Update validation timestamp in database
    }

    /**
     * Clear validation cache for a single credit bureau.
     * 
     * @param creditBureauId the credit bureau ID
     */
    @Override
    @CacheEvict(value = "configValidationCache", key = "#creditBureauId")
    public void clearValidationCache(Long creditBureauId) {
        log.info("Cleared validation cache for credit bureau: {}", creditBureauId);
    }

    /**
     * Clear all validation caches.
     */
    @Override
    @CacheEvict(value = "configValidationCache", allEntries = true)
    public void clearAllValidationCaches() {
        log.info("Cleared all configuration validation caches");
    }

    /**
     * Helper method to build validation report.
     */
    private ConfigurationValidationReport buildReport(Long creditBureauId, String bureauName,
            List<String> errors, List<String> warnings, boolean isValid,
            boolean hasRequiredFields, boolean credentialsValid, long duration) {
        return ConfigurationValidationReport.builder()
            .creditBureauId(creditBureauId)
            .creditBureauName(bureauName)
            .isValid(isValid)
            .hasRequiredFields(hasRequiredFields)
            .credentialsValid(credentialsValid)
            .connectionEstablished(isValid)
            .errors(errors)
            .warnings(warnings)
            .validatedAt(LocalDateTime.now().toString())
            .lastValidationStatus(isValid ? "SUCCESS" : "FAILED")
            .validationDurationMs(duration)
            .build();
    }
}
