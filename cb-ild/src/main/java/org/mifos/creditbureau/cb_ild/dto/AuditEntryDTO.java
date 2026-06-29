package org.mifos.creditbureau.cb_ild.dto;

import org.mifos.creditbureau.cb_ild.entity.AuditEntry;

import java.time.LocalDateTime;

/**
 * Safe DTO for GET /api/clients/{id}/audit-trail.
 *
 * NEVER exposes:
 *   oldValue/newValue — may contain sensitive data
 *   errorMessage      — may contain internal details
 *
 * COMPLIANCE role only — Angular Tab 5.
 */
public record AuditEntryDTO(
        Long id,
        Long clientId,
        String action,
        String entityType,
        String performedBy,
        String requestId,
        String result,
        Long durationMs,
        LocalDateTime createdAt
) {
    public static AuditEntryDTO from(AuditEntry entry) {
        return new AuditEntryDTO(
                entry.getId(),
                entry.getClientId(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getPerformedBy(),
                entry.getRequestId(),
                entry.getResult(),
                entry.getDurationMs(),
                entry.getCreatedAt()
        );
    }
}
