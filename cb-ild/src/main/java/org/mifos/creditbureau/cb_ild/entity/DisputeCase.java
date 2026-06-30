package org.mifos.creditbureau.cb_ild.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;
import org.mifos.creditbureau.cb_ild.entity.enums.DisputeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for dispute_case table — a dispute raised against a specific
 * CDC submission's outcome.
 *
 * V3 columns:
 *   id, submission_record_id, status, dispute_details,
 *   institution_data, cdc_data, opened_at, resolved_at, soft_deleted
 *
 * V9 columns:
 *   raised_by, resolution_notes, expiry_date
 *   (opened_at made NOT NULL in V9)
 *
 * Client linkage:
 *   A dispute is tied to a specific submission_record (submissionRecordId),
 *   NOT directly to a client. To find disputes for a client, join through
 *   submission_record.client_id. A dispute is about "this CDC submission's
 *   result", not the client in the abstract — a client may have many
 *   submissions over time, each independently disputable.
 *
 * State machine (enforced in DisputeServiceImpl, see DisputeStatus):
 *   OPEN -> UNDER_REVIEW (any role) -> RESOLVED (COMPLIANCE only)
 *   No backwards transitions.
 *
 * Side-by-side snapshot:
 *   institutionData = Fineract client data snapshot at dispute creation time
 *   cdcData         = bureau_response snapshot at dispute creation time
 *   These are frozen at OPEN time so the dispute reflects what was actually
 *   compared, even if the underlying records change later.
 *
 * Compliance:
 *   expiryDate = LRSIC 72-month retention boundary for this dispute record.
 *   Never hard-delete — softDeleted=true only.
 *   @SQLRestriction ensures soft-deleted rows never returned by default queries.
 *
 * raisedBy:
 *   userId from MDC (correlation context) at the time the dispute was created.
 */
@Entity
@Table(name = "dispute_case")
@SQLRestriction("soft_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DisputeCase {

    // ===== V3 COLUMNS =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Links this dispute to the specific submission whose result is being
    // disputed. Join to submission_record.client_id to find a client's disputes.
    @Column(name = "submission_record_id", nullable = false)
    private Long submissionRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private DisputeStatus status;

    // Free-text description of what's being disputed and why.
    @Column(name = "dispute_details", columnDefinition = "TEXT")
    private String disputeDetails;

    // Snapshot of Fineract client data at dispute creation time.
    @Column(name = "institution_data", columnDefinition = "TEXT")
    private String institutionData;

    // Snapshot of bureau_response (CDC) data at dispute creation time.
    @Column(name = "cdc_data", columnDefinition = "TEXT")
    private String cdcData;

    // Set when the dispute is created. NOT NULL since V9.
    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    // Set when status transitions to RESOLVED. Null until then.
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // Never hard-delete — always softDeleted=true
    @Builder.Default
    @Column(name = "soft_deleted")
    private Boolean softDeleted = false;

    // ===== V9 COLUMNS =====

    // userId from MDC at the time this dispute was created.
    @Column(name = "raised_by", nullable = false, length = 255)
    private String raisedBy;

    // Filled in when status transitions to RESOLVED. Compliance notes
    // explaining how the dispute was resolved.
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    // LRSIC 72-month retention boundary. LocalDate, matching the
    // expiry_date convention established in BureauResponseEntity and
    // SubmissionRecord.
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
}
