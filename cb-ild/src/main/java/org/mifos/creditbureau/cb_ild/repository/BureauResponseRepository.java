package org.mifos.creditbureau.cb_ild.repository;

import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository for bureau_response table.
 *
 * @SQLRestriction on BureauResponseEntity ensures soft_deleted=false
 * automatically on every query — no manual filter needed here.
 *
 * Used by:
 *   CdcScorePullServiceImpl  — save + score drop detection
 *   BureauReadinessController (Week 4) — latest score per client
 *   Angular Tab 1 — latest FICO score + riskBand
 *   Angular Tab 3 — score history + delinquency data
 *   Angular Tab 4 — tradelines + alerts
 */
@Repository
public interface BureauResponseRepository
        extends JpaRepository<BureauResponseEntity, Long> {

    /**
     * Latest non-deleted response for a client.
     *
     * Used by:
     *   CdcScorePullServiceImpl — score drop detection
     *   Angular Tab 1 — display latest FICO score
     */
    Optional<BureauResponseEntity>
        findTopByClientIdOrderByPulledAtDesc(Long clientId);

    /**
     * All non-deleted responses for a client — newest first.
     *
     * Used by:
     *   Angular Tab 3 — score history chart x-axis
     *   RetentionService (Week 4) — 72-month countdown
     */
    List<BureauResponseEntity>
        findAllByClientIdOrderByPulledAtDesc(Long clientId);

    /**
     * All non-deleted rows where expiryDate is before the given date.
     * Used by RetentionService — finds rows eligible for archiving.
     * @SQLRestriction adds soft_deleted=false automatically.
     * Rows with null expiryDate excluded by JPA automatically.
     *
     * SQL: SELECT * FROM bureau_response
     *      WHERE expiry_date < ? AND soft_deleted = false
     */
    List<BureauResponseEntity>
        findAllByExpiryDateBefore(LocalDate date);
}
