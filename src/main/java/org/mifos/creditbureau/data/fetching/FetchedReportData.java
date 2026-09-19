package org.mifos.creditbureau.data.fetching;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents fetched credit report data from a credit bureau.
 * Stores the raw report response and metadata about when it was fetched.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FetchedReportData {

    /**
     * Unique identifier for this fetched report record.
     */
    private Long id;

    /**
     * ID of the client this report is for.
     */
    private Long clientId;

    /**
     * Name of the client.
     */
    private String clientName;

    /**
     * ID of the credit bureau that provided this report.
     */
    private Long creditBureauId;

    /**
     * Name of the credit bureau.
     */
    private String creditBureauName;

    /**
     * Raw JSON response from credit bureau API.
     */
    private String rawReportJson;

    /**
     * HTTP status code returned by credit bureau API.
     */
    private Integer httpStatusCode;

    /**
     * Timestamp when report was fetched.
     */
    private LocalDateTime fetchedAt;

    /**
     * Report validity period start date (if provided by CB).
     */
    private LocalDateTime validFrom;

    /**
     * Report validity period end date (if provided by CB).
     */
    private LocalDateTime validUntil;

    /**
     * Whether the report is currently valid/active.
     */
    private boolean isValid;

    /**
     * Request metadata sent to credit bureau.
     */
    private String requestMetadata;

    /**
     * Response headers from credit bureau API.
     */
    @Builder.Default
    private Map<String, String> responseHeaders = new HashMap<>();

    /**
     * Whether report was successfully parsed and stored.
     */
    private boolean successfullyProcessed;

    /**
     * Error message if parsing/storage failed.
     */
    private String processingErrorMessage;

    /**
     * Credit score extracted from report (if applicable).
     */
    private Integer creditScore;

    /**
     * Parsed report details (JSON map for flexibility).
     */
    @Builder.Default
    private Map<String, Object> parsedReportData = new HashMap<>();

    /**
     * Flag to indicate if report needs re-validation.
     */
    private boolean needsRevalidation;

    /**
     * Last validation timestamp.
     */
    private LocalDateTime lastValidatedAt;
}
