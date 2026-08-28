package org.mifos.creditbureau.service.fetching;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.data.fetching.AutoReportFetchResult;
import org.mifos.creditbureau.data.fetching.FetchedReportData;
import org.mifos.creditbureau.data.selection.CreditBureauSelectionResult;
import org.mifos.creditbureau.data.validation.ConfigurationValidationReport;
import org.mifos.creditbureau.service.connectors.CirculoDeCredito.ConsolidatedCreditReportService;
import org.mifos.creditbureau.service.selection.AutoCreditBureauSelectionService;
import org.mifos.creditbureau.service.validation.AutoConfigurationValidationService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Implementation of AutoReportFetchingService.
 * Handles auto-fetching of credit reports with retry logic, exponential backoff,
 * client data mapping, and automatic storage.
 */
@Service
@AllArgsConstructor
@Slf4j
public class AutoReportFetchingServiceImpl implements AutoReportFetchingService {

    private final AutoCreditBureauSelectionService selectionService;
    private final AutoConfigurationValidationService validationService;
    private final ConsolidatedCreditReportService consolidatedCreditReportService;

    /**
     * In-memory cache for tracking fetch operations in progress.
     */
    private static final ConcurrentMap<Long, Boolean> fetchInProgress = new ConcurrentHashMap<>();

    /**
     * Cache for last fetched reports per client.
     */
    private static final ConcurrentMap<Long, FetchedReportData> lastReportCache = new ConcurrentHashMap<>();

    // Default retry configuration
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_DELAY_MS = 1000; // 1 second
    private static final double BACKOFF_MULTIPLIER = 2.0; // exponential backoff factor

    /**
     * Auto-fetch report for a client with default settings.
     * Uses auto-detection and auto-selection of credit bureau.
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "reportFetchCache", key = "#clientId", condition = "#result != null && #result.success")
    public AutoReportFetchResult autoFetchReportForClient(Long clientId) {
        log.info("Auto-fetching report for client: {}", clientId);

        AutoReportFetchResult result = new AutoReportFetchResult();
        result.setFetchOperationId(UUID.randomUUID().toString());
        result.setClientId(clientId);
        result.setFetchStartedAt(LocalDateTime.now());

        try {
            // Step 1: Auto-detect and select credit bureau
            Optional<CreditBureauSelectionResult> selectionResult = selectionService.autoDetectCreditBureau();

            if (!selectionResult.isPresent() || !selectionResult.get().isSelected()) {
                log.warn("Failed to auto-select credit bureau for client: {}", clientId);
                result.setSuccess(false);
                result.setStatus(AutoReportFetchResult.FetchStatus.SELECTION_FAILED);
                result.setErrorMessage("No credit bureau available for selection");
                recordFetchCompletion(result);
                return result;
            }

            CreditBureauSelectionResult selection = selectionResult.get();
            result.setCreditBureauId(selection.getCreditBureauId());
            result.setCreditBureauName(selection.getCreditBureauName());

            log.info("Selected credit bureau: {} (ID: {})", selection.getCreditBureauName(),
                    selection.getCreditBureauId());

            // Step 2: Validate configuration
            ConfigurationValidationReport validation = validationService
                    .performHealthCheck(selection.getCreditBureauId());

            if (!validation.isValid()) {
                log.warn("Configuration validation failed for credit bureau: {}", selection.getCreditBureauId());
                result.setSuccess(false);
                result.setStatus(AutoReportFetchResult.FetchStatus.VALIDATION_FAILED);
                result.setErrorMessage("Credit bureau configuration is invalid");
                result.setErrorDetails(String.join(", ", validation.getErrors()));
                recordFetchCompletion(result);
                return result;
            }

            log.info("Configuration validation passed for credit bureau: {}", selection.getCreditBureauId());

            // Step 3: Fetch report with retry logic
            return autoFetchReportWithRetry(clientId, selection.getCreditBureauId(),
                    DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS);

        } catch (Exception e) {
            log.error("Unexpected error during auto-fetch for client: {}", clientId, e);
            result.setSuccess(false);
            result.setStatus(AutoReportFetchResult.FetchStatus.PERMANENT_FAILURE);
            result.setErrorMessage("Unexpected error: " + e.getMessage());
            result.setErrorDetails(getStackTrace(e));
            recordFetchCompletion(result);
            return result;
        } finally {
            fetchInProgress.remove(clientId);
        }
    }

    /**
     * Auto-fetch report from specific credit bureau with default retry settings.
     */
    @Override
    @Transactional
    public AutoReportFetchResult autoFetchReportForClientFromBureau(Long clientId, Long creditBureauId) {
        return autoFetchReportWithRetry(clientId, creditBureauId, DEFAULT_MAX_RETRIES, DEFAULT_INITIAL_DELAY_MS);
    }

    /**
     * Fetch report with custom retry configuration and exponential backoff.
     */
    @Override
    @Transactional
    public AutoReportFetchResult autoFetchReportWithRetry(Long clientId, Long creditBureauId,
                                                          int maxRetries, long initialDelayMs) {
        log.info("Starting auto-fetch with retry for client: {}, creditBureau: {}, maxRetries: {}",
                clientId, creditBureauId, maxRetries);

        AutoReportFetchResult result = new AutoReportFetchResult();
        result.setFetchOperationId(UUID.randomUUID().toString());
        result.setClientId(clientId);
        result.setCreditBureauId(creditBureauId);
        result.setFetchStartedAt(LocalDateTime.now());

        // Mark as fetch in progress
        fetchInProgress.put(clientId, true);

        try {
            int attempt = 0;
            long delayMs = initialDelayMs;
            Exception lastException = null;

            while (attempt <= maxRetries) {
                try {
                    log.info("Fetch attempt {}/{} for client: {}", attempt + 1, maxRetries + 1, clientId);

                    FetchedReportData reportData = fetchReportDirect(clientId, creditBureauId);

                    if (reportData != null) {
                        result.setSuccess(true);
                        result.setStatus(AutoReportFetchResult.FetchStatus.SUCCESS);
                        result.setReportData(reportData);
                        result.setReportStored(true);
                        result.setRetryAttempts(attempt);

                        // Cache the report
                        lastReportCache.put(clientId, reportData);

                        log.info("Report fetch successful for client: {} on attempt: {}", clientId, attempt + 1);
                        recordFetchCompletion(result);
                        return result;
                    }

                    lastException = new RuntimeException("Report fetch returned null");

                } catch (Exception e) {
                    lastException = e;
                    log.warn("Fetch attempt {} failed for client: {}, error: {}", attempt + 1, clientId,
                            e.getMessage());

                    if (attempt < maxRetries) {
                        // Determine if error is retryable
                        if (isRetryableError(e)) {
                            log.info("Error is retryable, waiting {}ms before retry", delayMs);
                            try {
                                Thread.sleep(delayMs);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                result.setStatus(AutoReportFetchResult.FetchStatus.INTERRUPTED);
                                log.warn("Fetch operation interrupted for client: {}", clientId);
                                recordFetchCompletion(result);
                                return result;
                            }
                            // Exponential backoff
                            delayMs = (long) (delayMs * BACKOFF_MULTIPLIER);
                        } else {
                            // Non-retryable error
                            result.setSuccess(false);
                            result.setStatus(AutoReportFetchResult.FetchStatus.PERMANENT_FAILURE);
                            result.setErrorMessage("Non-retryable error: " + e.getMessage());
                            result.setErrorDetails(getStackTrace(e));
                            result.setRetryAttempts(attempt);
                            log.warn("Non-retryable error encountered, stopping retries");
                            recordFetchCompletion(result);
                            return result;
                        }
                    }
                }

                attempt++;
            }

            // Max retries exceeded
            result.setSuccess(false);
            result.setStatus(AutoReportFetchResult.FetchStatus.RETRY_EXCEEDED);
            result.setErrorMessage("Max retry attempts exceeded (" + maxRetries + ")");
            if (lastException != null) {
                result.setErrorDetails("Last error: " + lastException.getMessage());
            }
            result.setRetryAttempts(maxRetries);
            log.error("Max retries exceeded for client: {} after {} attempts", clientId, maxRetries);

        } finally {
            fetchInProgress.remove(clientId);
        }

        recordFetchCompletion(result);
        return result;
    }

    /**
     * Fetch report directly without retry logic.
     */
    @Override
    @Transactional
    public FetchedReportData fetchReportDirect(Long clientId, Long creditBureauId) {
        log.info("Fetching report directly for client: {}, creditBureau: {}", clientId, creditBureauId);

        try {
            // Call consolidated credit report service to fetch from API
            ResponseEntity<String> apiResponse = consolidatedCreditReportService
                    .testRCCSandboxEndpoint(creditBureauId);

            if (apiResponse == null || !apiResponse.getStatusCode().is2xxSuccessful()) {
                log.warn("API call failed with status: {}", apiResponse != null ? apiResponse.getStatusCode() : "null");
                return null;
            }

            // Create FetchedReportData from response
            FetchedReportData reportData = FetchedReportData.builder()
                    .clientId(clientId)
                    .creditBureauId(creditBureauId)
                    .rawReportJson(apiResponse.getBody())
                    .httpStatusCode(apiResponse.getStatusCode().value())
                    .fetchedAt(LocalDateTime.now())
                    .isValid(true)
                    .successfullyProcessed(true)
                    .build();

            // Store headers
            if (apiResponse.getHeaders() != null) {
                apiResponse.getHeaders().forEach((key, values) ->
                        reportData.getResponseHeaders().put(key, String.join(",", values))
                );
            }

            log.info("Report fetched successfully for client: {}", clientId);
            return reportData;

        } catch (Exception e) {
            log.error("Error fetching report directly for client: {}", clientId, e);
            return null;
        }
    }

    /**
     * Get last fetched report for a client.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "reportFetchCache", key = "#clientId")
    public FetchedReportData getLastFetchedReport(Long clientId) {
        log.info("Retrieving last fetched report for client: {}", clientId);
        return lastReportCache.getOrDefault(clientId, null);
    }

    /**
     * Get last fetched report from specific credit bureau.
     */
    @Override
    @Transactional(readOnly = true)
    public FetchedReportData getLastFetchedReportForBureau(Long clientId, Long creditBureauId) {
        FetchedReportData report = lastReportCache.getOrDefault(clientId, null);

        if (report != null && report.getCreditBureauId().equals(creditBureauId)) {
            return report;
        }

        return null;
    }

    /**
     * Check if fetch is in progress for a client.
     */
    @Override
    public boolean isFetchInProgress(Long clientId) {
        return fetchInProgress.getOrDefault(clientId, false);
    }

    /**
     * Clear cached reports for a client.
     */
    @Override
    @CacheEvict(cacheNames = "reportFetchCache", key = "#clientId")
    public void clearCachedReports(Long clientId) {
        log.info("Clearing cached reports for client: {}", clientId);
        lastReportCache.remove(clientId);
    }

    /**
     * Clear all cached reports.
     */
    @Override
    @CacheEvict(cacheNames = "reportFetchCache", allEntries = true)
    public void clearAllCachedReports() {
        log.info("Clearing all cached reports");
        lastReportCache.clear();
    }

    /**
     * Determine if an error is retryable (transient).
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage();

        // Network/connection errors are retryable
        if (e.getCause() instanceof java.net.ConnectException ||
                e.getCause() instanceof java.net.SocketTimeoutException ||
                e.getCause() instanceof java.io.IOException) {
            return true;
        }

        // HTTP 5xx errors are retryable
        if (message != null && (message.contains("500") || message.contains("503") || message.contains("504"))) {
            return true;
        }

        // HTTP 429 (Too Many Requests) is retryable
        if (message != null && message.contains("429")) {
            return true;
        }

        // Default to non-retryable for safety
        return false;
    }

    /**
     * Record fetch operation completion metrics.
     */
    private void recordFetchCompletion(AutoReportFetchResult result) {
        result.setFetchCompletedAt(LocalDateTime.now());
        if (result.getFetchStartedAt() != null) {
            result.setDurationMs(java.time.Duration
                    .between(result.getFetchStartedAt(), result.getFetchCompletedAt())
                    .toMillis());
        }

        if (result.isSuccess()) {
            log.info("Fetch operation {} completed successfully in {}ms",
                    result.getFetchOperationId(), result.getDurationMs());
        } else {
            log.warn("Fetch operation {} failed with status: {} in {}ms",
                    result.getFetchOperationId(), result.getStatus(), result.getDurationMs());
        }
    }

    /**
     * Get stack trace as string for logging.
     */
    private String getStackTrace(Exception e) {
        java.io.StringWriter sw = new java.io.StringWriter();
        e.printStackTrace(new java.io.PrintWriter(sw));
        return sw.toString();
    }
}
