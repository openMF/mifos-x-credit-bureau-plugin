package org.mifos.creditbureau.cb_ild.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for submission_record table — tracks every CDC submission attempt.
 *
 * V2 columns:
 *   id, client_id, loan_id, trigger_type, status, rejection_reason,
 *   retry_count, next_retry_at, submitted_at, updated_at, soft_deleted
 *
 * V9 columns:
 *   created_at, cdc_reference_id, inquiry_type, expiry_date
 *
 * Lifecycle:
 *   PENDING -> ACCEPTED | REJECTED | PARTIAL | PENDING_RETRY
 *   PENDING_RETRY -> ACCEPTED | PENDING_RETRY (retryCount++) | PERMANENTLY_FAILED
 *
 * Retry handling (SubmissionRetryScheduler, MX-274):
 *   retryCount tracks attempts so far (0, 1, 2). At retryCount == 3,
 *   status becomes PERMANENTLY_FAILED and nextRetryAt is no longer updated.
 *   nextRetryAt uses exponential backoff:
 *     nextRetryAt = now + (retryCount² × retryIntervalMinutes)
 *     retryCount=1 → 60min, retryCount=2 → 240min, retryCount=3 → 540min
 *
 * Compliance:
 *   expiryDate = LRSIC 72-month retention boundary for this record.
 *   Never hard-delete — softDeleted=true only.
 *   @SQLRestriction ensures soft-deleted rows never returned by default queries.
 *
 * inquiryType:
 *   "HARD" or "SOFT" — required when triggerType = SCREENING_EVENT
 *   (LRSIC inquiry logging rule, MX-276 POST /api/submissions/report-screening).
 *   Null for LOAN_APPROVAL and MANUAL_BATCH triggers.
 */
@Entity
@Table(name = "submission_record")
@SQLRestriction("soft_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRecord {

    // ===== V2 COLUMNS =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    @Column(name = "loan_id")
    private Long loanId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 50)
    private TriggerType triggerType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private SubmissionStatus status;

    // Set when status = REJECTED. Why CDC rejected this submission.
    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // Number of retry attempts so far. 0 = first attempt, max 3 before
    // PERMANENTLY_FAILED. Incremented by SubmissionRetryScheduler.
    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    // When SubmissionRetryScheduler should next attempt this record.
    // Only meaningful when status = PENDING_RETRY.
    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    // When this submission was actually sent to CDC. Null until first attempt.
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // Manually updated whenever status changes — NOT @UpdateTimestamp,
    // set explicitly in SubmissionServiceImpl so retries control this value.
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Never hard-delete — always softDeleted=true
    @Builder.Default
    @Column(name = "soft_deleted")
    private Boolean softDeleted = false;

    // ===== V9 COLUMNS =====

    // Auto-set on insert — never set manually
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // CDC's own reference ID for this submission. Set when status = ACCEPTED.
    @Column(name = "cdc_reference_id", length = 255)
    private String cdcReferenceId;

    // "HARD" or "SOFT" — required when triggerType = SCREENING_EVENT.
    @Column(name = "inquiry_type", length = 20)
    private String inquiryType;

    // LRSIC 72-month retention boundary. LocalDate (not LocalDateTime) —
    // matches the expiry_date convention established in BureauResponseEntity.
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
