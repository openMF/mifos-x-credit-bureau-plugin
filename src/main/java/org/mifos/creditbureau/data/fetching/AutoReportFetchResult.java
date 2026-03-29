package org.mifos.creditbureau.data.fetching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of an auto-report fetch operation.
 * Contains status, success/failure info, and fetched report data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutoReportFetchResult {

    /**
     * Unique identifier for this fetch operation.
     */
    private String fetchOperationId;

    /**
     * ID of the client for which report was fetched.
     */
    private Long clientId;

    /**
     * ID of the credit bureau from which report was fetched.
     */
    private Long creditBureauId;

    /**
     * Name of the credit bureau.
     */
    private String creditBureauName;

    /**
     * Whether the fetch was successful.
     */
    private boolean success;

    /**
     * HTTP status code returned by credit bureau API (if applicable).
     */
    private Integer httpStatusCode;

    /**
     * Result status: SUCCESS, TEMPORARY_FAILURE, PERMANENT_FAILURE, RETRY_EXCEEDED
     */
    private FetchStatus status;

    /**
     * Number of retry attempts made.
     */
    private int retryAttempts;

    /**
     * Error message if fetch failed.
     */
    private String errorMessage;

    /**
     * Detailed error description with stack trace info.
     */
    private String errorDetails;

    /**
     * The fetched report data (null if fetch failed).
     */
    private FetchedReportData reportData;

    /**
     * Timestamp when the fetch operation started.
     */
    private LocalDateTime fetchStartedAt;

    /**
     * Timestamp when the fetch operation completed.
     */
    private LocalDateTime fetchCompletedAt;

    /**
     * Duration of fetch operation in milliseconds.
     */
    private long durationMs;

    /**
     * Whether the report was stored in the database.
     */
    private boolean reportStored;

    /**
     * ID of the stored report in database (if stored).
     */
    private Long storedReportId;

    /**
     * Additional metadata about the fetch.
     */
    private String metadata;

    /**
     * Enumeration for fetch result statuses.
     */
    public enum FetchStatus {
        SUCCESS("Report fetched successfully"),
        TEMPORARY_FAILURE("Temporary failure, will retry"),
        PERMANENT_FAILURE("Permanent failure, cannot retry"),
        RETRY_EXCEEDED("Max retry attempts exceeded"),
        VALIDATION_FAILED("Configuration validation failed"),
        SELECTION_FAILED("Credit bureau selection failed"),
        INTERRUPTED("Fetch operation was interrupted");

        private final String description;

        FetchStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
