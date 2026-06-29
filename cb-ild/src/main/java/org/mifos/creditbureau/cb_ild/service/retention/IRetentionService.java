package org.mifos.creditbureau.cb_ild.service.retention;

/**
 * SOLID interface for LRSIC retention service.
 *
 * Runs nightly at 2am via @Scheduled.
 * Archives expired bureau_response rows per LRSIC 72-month rule.
 *
 * Phase 1 behavior:
 *   All rows have expiryDate=null in mock mode.
 *   findAllByExpiryDateBefore returns empty list.
 *   Logs "no expired records found" and returns.
 *   Zero rows archived in Phase 1 — correct.
 *
 * Phase 2 behavior:
 *   Real CDC sets dateOfFirstDelinquency.
 *   expiryDate = dateOfFirstDelinquency + 72 months.
 *   Rows expire and get archived automatically.
 */
public interface IRetentionService {
    void archiveExpiredRecords();
}
