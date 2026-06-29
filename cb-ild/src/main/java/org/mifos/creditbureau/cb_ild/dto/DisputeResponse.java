package org.mifos.creditbureau.cb_ild.dto;

import org.mifos.creditbureau.cb_ild.entity.DisputeCase;
import org.mifos.creditbureau.cb_ild.entity.enums.DisputeStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Clean DTO for DisputeCase — returned by DisputeController (MX-276).
 *
 * Why not return DisputeCase entity directly:
 *   - Exposes softDeleted (internal field)
 *   - Exposes institutionData + cdcData as raw JSON blobs (large, PII risk)
 *   - JPA entity serialization can trigger lazy loading issues
 *
 * What this DTO exposes:
 *   - All fields Angular needs for Tab 5 (Dispute Management)
 *   - institutionDataSummary: first 200 chars of institutionData
 *   - cdcDataSummary: first 200 chars of cdcData
 *   - No softDeleted, no raw blobs
 *
 * Java 21 record — immutable, no boilerplate (RULE 02).
 */
public record DisputeResponse(
        Long id,
        Long submissionRecordId,
        DisputeStatus status,
        String disputeDetails,
        String raisedBy,
        LocalDateTime openedAt,
        LocalDateTime resolvedAt,
        String resolutionNotes,
        LocalDate expiryDate,
        String institutionDataSummary,
        String cdcDataSummary
) {

    /**
     * Maps DisputeCase entity to DisputeResponse DTO.
     * Truncates raw snapshot fields to 200 chars for display.
     *
     * @param entity the DisputeCase entity from the database
     * @return clean DTO safe for Angular consumption
     */
    public static DisputeResponse from(DisputeCase entity) {
        return new DisputeResponse(
                entity.getId(),
                entity.getSubmissionRecordId(),
                entity.getStatus(),
                entity.getDisputeDetails(),
                entity.getRaisedBy(),
                entity.getOpenedAt(),
                entity.getResolvedAt(),
                entity.getResolutionNotes(),
                entity.getExpiryDate(),
                truncate(entity.getInstitutionData()),
                truncate(entity.getCdcData())
        );
    }

    /**
     * Truncates a string to 200 characters for display.
     * Returns null if input is null.
     */
    private static String truncate(String value) {
        if (value == null) return null;
        return value.length() <= 200 ? value : value.substring(0, 200) + "...";
    }
}
