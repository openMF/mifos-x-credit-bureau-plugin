package org.mifos.creditbureau.cb_ild.service.bureau;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.service.cdc.ICdcScorePullService;
import org.mifos.creditbureau.cb_ild.service.kyc.IKycScoringService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;

/**
 * Orchestrates the full bureau readiness check pipeline.
 *
 * Pipeline:
 *   Step 1: FineractApiClient.getClientData()  — all 3 Fineract endpoints
 *   Step 2: KycCompletenessScorer.score()      — weighted KYC score
 *   Step 3: score < 70  → return immediately, CDC never called
 *   Step 4: score >= 70 → CdcScorePullService.pullAndSave()
 *   Step 5: Map BureauResponseEntity → final KycReadinessResult
 *
 * Field mapping BureauResponseEntity → KycReadinessResult:
 *   getFicoScore()      Integer       → ficoScore
 *   getRiskBand()       String        → riskBand
 *   getScoreDropAlert() Boolean       → scoreDropAlert (null-safe)
 *   getPulledAt()       LocalDateTime → pulledAt Instant (UTC)
 *
 * Security:
 *   Never logs RFC value — clientId only
 *   Never logs FICO score value
 */
@Slf4j
@Service
public class BureauReadinessService implements IBureauReadinessService {

    private final FineractApiClient fineractApiClient;
    private final IKycScoringService kycScoringService;
    private final ICdcScorePullService cdcScorePullService;

    public BureauReadinessService(
            FineractApiClient fineractApiClient,
            IKycScoringService kycScoringService,
            ICdcScorePullService cdcScorePullService) {
        this.fineractApiClient = fineractApiClient;
        this.kycScoringService = kycScoringService;
        this.cdcScorePullService = cdcScorePullService;
    }

    @Override
    public KycReadinessResult checkReadiness(Long clientId) {

        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }

        log.info("Bureau readiness check — clientId: {}", clientId);

        // Step 1 — fetch all 3 Fineract endpoints
        FineractClientData clientData =
                fineractApiClient.getClientData(clientId);

        // Step 2 — KYC score
        // RFC missing: returns score=0, ready=false — no exception
        KycReadinessResult kycResult = kycScoringService.score(clientData);

        log.debug("KYC result — clientId: {}, score: {}, ready: {}",
                clientId, kycResult.score(), kycResult.ready());

        // Step 3 — score below threshold → skip CDC
        if (!kycResult.ready()) {
            log.info("Score below threshold — CDC not called for clientId: {}",
                    clientId);
            return kycResult;
        }

        // Step 4 — CDC pull (mock or real)
        BureauResponseEntity bureauResponse =
                cdcScorePullService.pullAndSave(clientId);

        // Step 5 — map entity → final KycReadinessResult
        // pulledAt: LocalDateTime → Instant via UTC zone
        // scoreDropAlert: Boolean (boxed) — null-safe
        return new KycReadinessResult(
                clientId,
                kycResult.score(),
                kycResult.ready(),
                kycResult.missingFields(),
                bureauResponse.getFicoScore(),
                bureauResponse.getRiskBand(),
                bureauResponse.getScoreDropAlert() != null
                        && bureauResponse.getScoreDropAlert(),
                bureauResponse.getPulledAt() != null
                        ? bureauResponse.getPulledAt()
                                .atZone(ZoneOffset.UTC)
                                .toInstant()
                        : null
        );
    }
}
