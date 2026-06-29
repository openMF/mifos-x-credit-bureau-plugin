package org.mifos.creditbureau.cb_ild.entity.enums;

/**
 * Lifecycle status of a single CDC submission attempt.
 *
 * PENDING             — created, not yet sent to CDC.
 * ACCEPTED            — CDC accepted the submission. cdc_reference_id is set.
 * REJECTED            — CDC rejected the submission outright (e.g. data validation
 *                        error). Never retried — rejection_reason is set.
 * PARTIAL             — CDC accepted some records in a batch but rejected others.
 *                        Used by runBatch() to summarize mixed-result batches.
 * PENDING_RETRY       — CDC call failed for a retryable reason (timeout, 503).
 *                        Will be retried by SubmissionRetryScheduler, up to 3 attempts
 *                        total (retry_count tracks attempts so far).
 * PERMANENTLY_FAILED  — retry_count reached 3. No further automatic retries.
 *                        Requires manual intervention.
 */
public enum SubmissionStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    PARTIAL,
    PENDING_RETRY,
    PERMANENTLY_FAILED
}
