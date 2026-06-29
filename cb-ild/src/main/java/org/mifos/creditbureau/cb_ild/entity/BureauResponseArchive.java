package org.mifos.creditbureau.cb_ild.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * JPA entity for bureau_response_archive table — 5 columns.
 *
 * Rows moved here from bureau_response when:
 *   expiryDate < today (72 months after dateOfFirstDelinquency)
 *
 * LRSIC compliance:
 *   bureau_response rows are never hard-deleted.
 *   Expired rows archived here + softDeleted=true on original.
 *   Archive is proof the data existed — no full PII stored.
 *
 * Why only 5 columns (not all 15):
 *   fullResponse  — raw CDC PII — NEVER archived
 *   tradelines    — detailed credit history — NEVER archived
 *   Only summary: clientId, ficoScore, pulledAt, archivedAt
 *
 * No @SQLRestriction — archive rows are never soft-deleted.
 * Archive is permanent — compliance requirement.
 */
@Entity
@Table(name = "bureau_response_archive")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BureauResponseArchive {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "fico_score")
    private Integer ficoScore;

    @Column(name = "pulled_at")
    private LocalDateTime pulledAt;

    // Auto-set when archive row created
    @CreationTimestamp
    @Column(name = "archived_at", updatable = false)
    private LocalDateTime archivedAt;
}
