package org.mifos.creditbureau.cb_ild.service.submission;

import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO returned by GET /api/submissions/history.
 * Angular Tab 2 receives this exactly.
 *
 * Java 21 record — immutable, compact (RULE 02).
 *
 * Follows the same pattern as KycReadinessResult (service/kyc/) — a
 * purpose-built response record living alongside its service, not in a
 * separate dto/ package (no dto/ package exists in this codebase; Phase 1
 * established response records as siblings of their service interface).
 *
 * Mapping is 1:1 field copy via the static from() factory — no MapStruct.
 * RULE 05 says "MapStruct for ALL complex object mappings", but Phase 1's
 * own KycReadinessResult is built via plain constructor calls
 * (BureauReadinessService.checkReadiness() Step 5) for an equivalent 1:1
 * mapping. A direct field copy with zero transformation logic does not
 * meet the bar for "complex" — introducing MapStruct here would be the
 * first usage in the codebase for something simpler than what Phase 1
 * already does without it.
 *
 * Unlike KycReadinessResult, SubmissionRecord has no PII fields to exclude
 * (no RFC, no DOB — those live in FineractClientData / BureauResponseEntity,
 * not here), so this is a complete 1:1 mapping of every entity field.
 *
 * status may be any SubmissionStatus value EXCEPT PARTIAL — submitClient()
 * never produces PARTIAL for a single record (see SubmissionServiceImpl
 * Javadoc). PARTIAL remains in the enum for a possible future batch-level
 * summary type, not used here.
 */
public record SubmissionRecordResponse(
        Long id,
        Long clientId,
        Long loanId,
        TriggerType triggerType,
        SubmissionStatus status,
        String rejectionReason,
        Integer retryCount,
        LocalDateTime nextRetryAt,
        LocalDateTime submittedAt,
        LocalDateTime updatedAt,
        LocalDateTime createdAt,
        String cdcReferenceId,
        String inquiryType,
        LocalDate expiryDate
) {

    /**
     * Maps a SubmissionRecord entity to its response DTO. 1:1 field copy.
     *
     * @param entity the entity to map — must not be null
     * @return the mapped response record
     */
    public static SubmissionRecordResponse from(SubmissionRecord entity) {
        return new SubmissionRecordResponse(
                entity.getId(),
                entity.getClientId(),
                entity.getLoanId(),
                entity.getTriggerType(),
                entity.getStatus(),
                entity.getRejectionReason(),
                entity.getRetryCount(),
                entity.getNextRetryAt(),
                entity.getSubmittedAt(),
                entity.getUpdatedAt(),
                entity.getCreatedAt(),
                entity.getCdcReferenceId(),
                entity.getInquiryType(),
                entity.getExpiryDate()
        );
    }
}
