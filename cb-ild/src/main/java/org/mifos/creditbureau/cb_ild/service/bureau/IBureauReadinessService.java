package org.mifos.creditbureau.cb_ild.service.bureau;

import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;

/**
 * SOLID interface for bureau readiness check.
 *
 * Orchestrates:
 *   FineractApiClient → KycCompletenessScorer → CdcScorePullService
 *
 * Throws:
 *   FineractNotFoundException   — client not in Fineract → 404
 *   FineractConnectionException — Fineract unreachable → 504
 *   FineractServerException     — Fineract 5xx → 503
 *   CdcNotConfiguredException   — real mode not configured → 503
 */
public interface IBureauReadinessService {
    KycReadinessResult checkReadiness(Long clientId);
}
