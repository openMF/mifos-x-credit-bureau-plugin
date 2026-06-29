package org.mifos.creditbureau.cb_ild.service.submission;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration for the CDC submission retry scheduler (MX-274).
 *
 * Mirrors KycScoringProperties — @ConfigurationProperties requires both
 * getters AND setters to bind values from application.properties. A
 * missing setter means the property silently falls back to its default
 * with no error (SE-09).
 *
 * Backoff formula:
 *   nextRetryAt = now + (retryCount^2 * retryIntervalMinutes)
 *
 *   retryCount 1 -> 1^2 * 60  =  60 minutes  (~1 hour)
 *   retryCount 2 -> 2^2 * 60  = 240 minutes  (~4 hours)
 *   retryCount 3 -> 3^2 * 60  = 540 minutes  (~9 hours), then PERMANENTLY_FAILED
 *
 * retryIntervalMinutes=60 chosen so backoff windows are meaningful relative
 * to the @Scheduled retry job running every 6 hours (SubmissionRetryScheduler,
 * MX-274) — a sub-minute backoff would be "always due" on every 6-hour run,
 * making the backoff functionally meaningless.
 *
 * pageSize is used by SubmissionRetryScheduler's paginated query
 * (findByStatusAndNextRetryAtBefore) — PE-02 requires Pageable, never
 * load-all-into-heap.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "cbild.schedule")
public class SubmissionRetryProperties {

    /**
     * Maximum retry attempts before a PENDING_RETRY record becomes
     * PERMANENTLY_FAILED. Default 3, matches MX-274 spec exactly.
     */
    private int maxAttempts = 3;

    /**
     * Base interval in minutes for the exponential backoff formula.
     * Default 60 (1 hour).
     */
    private int retryIntervalMinutes = 60;

    /**
     * Page size for SubmissionRetryScheduler's paginated query over
     * PENDING_RETRY records. Default 100 — never load all records into
     * heap (PE-02).
     */
    private int pageSize = 100;
}
