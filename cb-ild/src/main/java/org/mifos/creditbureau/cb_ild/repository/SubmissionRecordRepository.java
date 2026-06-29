package org.mifos.creditbureau.cb_ild.repository;

import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for submission_record table.
 *
 * @SQLRestriction on SubmissionRecord ensures soft_deleted=false
 * automatically on every query — no manual filter needed here.
 *
 * Used by:
 *   SubmissionServiceImpl (MX-273)    — save submissions, history lookup
 *   SubmissionRetryScheduler (MX-274) — find PENDING_RETRY records due for retry
 *   DisputeServiceImpl (MX-275)       — validate submissionRecordId exists before
 *                                        creating a dispute (via findById, inherited)
 *   Angular Tab 2 — GET /api/submissions/history
 */
@Repository
public interface SubmissionRecordRepository
        extends JpaRepository<SubmissionRecord, Long> {

    /**
     * Paginated submission history for a client, newest first.
     *
     * Used by:
     *   GET /api/submissions/history?clientId={id}
     *   Angular Tab 2 — submission history table
     *
     * SQL: SELECT * FROM submission_record
     *      WHERE client_id = ? AND soft_deleted = false
     *      ORDER BY submitted_at DESC
     */
    Page<SubmissionRecord>
        findByClientIdOrderBySubmittedAtDesc(Long clientId, Pageable pageable);

    /**
     * Records in PENDING_RETRY status whose next retry is due (next_retry_at
     * is in the past or now).
     *
     * Used by:
     *   SubmissionRetryScheduler (MX-274) — @Scheduled every 6 hours.
     *   Each result has retryCount checked; retryCount == 3 is moved to
     *   PERMANENTLY_FAILED instead of retried again.
     *
     * SQL: SELECT * FROM submission_record
     *      WHERE status = 'PENDING_RETRY' AND next_retry_at <= ?
     *        AND soft_deleted = false
     */
    Page<SubmissionRecord>
        findByStatusAndNextRetryAtBefore(SubmissionStatus status, LocalDateTime cutoff, Pageable pageable);

    /**
     * All non-deleted records with the given status, no ordering or paging.
     *
     * Used by:
     *   SubmissionServiceImpl.runBatch() — e.g. count PENDING records,
     *   or find all ACCEPTED records for a given day for reporting.
     *
     * SQL: SELECT * FROM submission_record
     *      WHERE status = ? AND soft_deleted = false
     */
    List<SubmissionRecord>
        findByStatus(SubmissionStatus status);
}
