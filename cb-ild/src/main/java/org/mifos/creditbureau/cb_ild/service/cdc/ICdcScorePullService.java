package org.mifos.creditbureau.cb_ild.service.cdc;

import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;

/**
 * Service interface for CDC score pull operations.
 *
 * SOLID — interface per service, implementation injected via constructor.
 *
 * Mock mode (mifos.cdc.mock.enabled=true):
 *   Saves ficoScore=750 — no real CDC call needed.
 *   Safe for development and CI.
 *
 * Real mode (Phase 2):
 *   Calls plugin → real CDC → maps response → saves to bureau_response.
 *   Requires: CDC credentials, plugin endpoint, Yu Wati confirmation.
 *
 * Used by:
 *   BureauReadinessController (Week 4)
 *   GET /api/clients/{id}/bureau-readiness → Angular Tab 1
 */
public interface ICdcScorePullService {

    /**
     * Pull CDC score for client and save to bureau_response table.
     *
     * Steps:
     *   1. Validate clientId not null
     *   2. Check mock mode flag
     *   3. Check previous score for score drop detection
     *   4. Compute SHA-256 rawResponseHash
     *   5. Save BureauResponseEntity to DB
     *   6. Return saved entity
     *
     * @param clientId Fineract client ID — must not be null
     * @return saved BureauResponseEntity
     * @throws IllegalArgumentException if clientId is null
     */
    BureauResponseEntity pullAndSave(Long clientId);
}
