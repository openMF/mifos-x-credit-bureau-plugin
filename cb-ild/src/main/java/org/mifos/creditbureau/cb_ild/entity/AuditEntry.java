package org.mifos.creditbureau.cb_ild.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity for audit_entry table — 12 columns.
 *
 * Audit entries are NEVER deleted — compliance requirement.
 * No @SQLRestriction — we always want all audit entries.
 * No softDeleted — audit trail must be complete.
 *
 * Columns:
 *   id, action, record_id, entity_type, old_value, new_value,
 *   performed_by, request_id, created_at (V4)
 *   duration_ms, result, error_message (V6)
 *
 * result values: SUCCESS or FAILURE
 * error_message truncated to 500 chars — never store raw PII
 *
 * Security:
 *   performed_by = userId from JWT — never RFC or raw credential
 *   request_id = correlation ID from MDC
 *   old_value/new_value — never store nationalId or raw CDC response
 */
@Entity
@Table(name = "audit_entry")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    // Never store nationalId or raw CDC response here
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    // Never store nationalId or raw CDC response here
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // userId from JWT — never RFC or raw credential
    @Column(name = "performed_by", length = 100)
    private String performedBy;

    // Correlation ID from MDC — set by CorrelationIdFilter
    @Column(name = "request_id", length = 100)
    private String requestId;

    // Fineract client ID — for audit trail lookup by client
    @Column(name = "client_id")
    private Long clientId;

    // Auto-set on insert — never set manually
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // V6 columns — added by V6__audit_entry_additions.xml

    // How long the operation took in milliseconds
    @Column(name = "duration_ms")
    private Long durationMs;

    // SUCCESS or FAILURE
    @Column(name = "result", length = 20)
    private String result;

    // Truncated error message — max 500 chars, never raw PII
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
}
