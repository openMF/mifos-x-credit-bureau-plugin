package org.mifos.creditbureau.cb_ild.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA entity for bureau_response table — 15 columns.
 *
 * V1 columns:
 *   id, client_id, bureau_type, full_response, fico_score,
 *   tradelines, alerts, pulled_at, expiry_date, soft_deleted
 *
 * V1b columns:
 *   risk_band, has_delinquencies, date_of_first_delinquency,
 *   raw_response_hash, score_drop_alert
 *
 * Frontend rules:
 *   ficoScore              → Angular Tab 1 + Tab 3
 *   riskBand               → Angular Tab 1 color badge
 *   scoreDropAlert         → Angular Tab 3 warning
 *   hasDelinquencies       → Angular Tab 3 retention
 *   dateOfFirstDelinquency → Angular Tab 3 72-month countdown
 *   tradelines             → Angular Tab 4
 *   alerts                 → Angular Tab 4
 *   pulledAt               → Angular Tab 1 + Tab 3 timestamp
 *
 * NEVER expose to Angular:
 *   fullResponse    → raw CDC response — PII
 *   rawResponseHash → internal audit only
 *   softDeleted     → internal only
 *
 * Compliance:
 *   expiryDate = dateOfFirstDelinquency + 72 months (LRSIC rule)
 *   Never hard-delete — softDeleted=true only
 *   @SQLRestriction ensures deleted rows never returned
 */
@Entity
@Table(name = "bureau_response")
@SQLRestriction("soft_deleted = false")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BureauResponseEntity {

    // ===== V1 COLUMNS =====

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Long clientId;

    // Values: "CIRCULO_DE_CREDITO"
    @Column(name = "bureau_type", nullable = false, length = 50)
    private String bureauType;

    // Raw CDC response — audit only, NEVER expose to Angular
    @Column(name = "full_response", columnDefinition = "TEXT")
    private String fullResponse;

    // FICO score — Angular Tab 1 + Tab 3
    @Column(name = "fico_score")
    private Integer ficoScore;

    // Credit accounts — Angular Tab 4
    @Column(name = "tradelines", columnDefinition = "TEXT")
    private String tradelines;

    // Monitoring alerts — Angular Tab 4
    @Column(name = "alerts", columnDefinition = "TEXT")
    private String alerts;

    // Auto-set on insert — never set manually
    @CreationTimestamp
    @Column(name = "pulled_at", nullable = false, updatable = false)
    private LocalDateTime pulledAt;

    // LRSIC 72-month rule — dateOfFirstDelinquency + 72 months
    // LocalDate (not LocalDateTime) — matches dateOfFirstDelinquency type
    // and avoids timezone conversion on MariaDB TIMESTAMP columns
    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    // Never hard-delete — always softDeleted=true
    @Builder.Default
    @Column(name = "soft_deleted")
    private Boolean softDeleted = false;

    // ===== V1b COLUMNS =====

    // Angular Tab 1 color badge — LOW/MEDIUM/HIGH/VERY_HIGH
    @Column(name = "risk_band", length = 20)
    private String riskBand;

    // Angular Tab 3 retention indicator
    @Builder.Default
    @Column(name = "has_delinquencies")
    private Boolean hasDelinquencies = false;

    // Required for LRSIC 72-month retention calculation
    // Angular Tab 3 countdown
    @Column(name = "date_of_first_delinquency")
    private LocalDate dateOfFirstDelinquency;

    // SHA-256 of raw CDC response — deduplication
    // NEVER expose to Angular
    @Column(name = "raw_response_hash", length = 64)
    private String rawResponseHash;

    // True when ficoScore dropped since last pull
    // Angular Tab 3 warning banner
    @Builder.Default
    @Column(name = "score_drop_alert")
    private Boolean scoreDropAlert = false;
}
