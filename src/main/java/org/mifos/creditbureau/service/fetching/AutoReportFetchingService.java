package org.mifos.creditbureau.service.fetching;

import org.mifos.creditbureau.data.fetching.AutoReportFetchResult;
import org.mifos.creditbureau.data.fetching.FetchedReportData;

/**
 * Service interface for automatically fetching credit bureau reports.
 * Handles auto-detection, auto-selection, client data mapping, retry logic,
 * and report storage without manual intervention.
 */
public interface AutoReportFetchingService {

    /**
     * Auto-fetch report for a specific client.
     * Automatically detects credit bureau, validates configuration,
     * maps client data, and stores report.
     *
     * @param clientId the ID of the client to fetch report for
     * @return AutoReportFetchResult with success/failure status
     */
    AutoReportFetchResult autoFetchReportForClient(Long clientId);

    /**
     * Auto-fetch report for a specific client from a specific credit bureau.
     * Includes retry logic with exponential backoff on transient failures.
     *
     * @param clientId the ID of the client to fetch report for
     * @param creditBureauId the ID of the credit bureau to fetch from
     * @return AutoReportFetchResult with success/failure status
     */
    AutoReportFetchResult autoFetchReportForClientFromBureau(Long clientId, Long creditBureauId);

    /**
     * Fetch report with custom retry configuration.
     *
     * @param clientId the ID of the client to fetch report for
     * @param creditBureauId the ID of the credit bureau to fetch from
     * @param maxRetries maximum number of retry attempts
     * @param initialDelayMs initial delay in milliseconds for exponential backoff
     * @return AutoReportFetchResult with success/failure status
     */
    AutoReportFetchResult autoFetchReportWithRetry(Long clientId, Long creditBureauId,
                                                    int maxRetries, long initialDelayMs);

    /**
     * Fetch report directly without retry logic.
     *
     * @param clientId the ID of the client to fetch report for
     * @param creditBureauId the ID of the credit bureau to fetch from
     * @return FetchedReportData on success, null on failure
     */
    FetchedReportData fetchReportDirect(Long clientId, Long creditBureauId);

    /**
     * Get the last fetched report for a client.
     *
     * @param clientId the ID of the client
     * @return FetchedReportData or null if no report found
     */
    FetchedReportData getLastFetchedReport(Long clientId);

    /**
     * Get the last fetched report for a client from specific credit bureau.
     *
     * @param clientId the ID of the client
     * @param creditBureauId the ID of the credit bureau
     * @return FetchedReportData or null if no report found
     */
    FetchedReportData getLastFetchedReportForBureau(Long clientId, Long creditBureauId);

    /**
     * Check if report fetch is in progress for a client.
     *
     * @param clientId the ID of the client
     * @return true if fetch is in progress, false otherwise
     */
    boolean isFetchInProgress(Long clientId);

    /**
     * Clear cached reports for a client.
     *
     * @param clientId the ID of the client
     */
    void clearCachedReports(Long clientId);

    /**
     * Clear all cached reports.
     */
    void clearAllCachedReports();
}
