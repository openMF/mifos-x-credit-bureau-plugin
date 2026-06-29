package org.mifos.creditbureau.cb_ild.service.submission;

import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * SOLID interface for the CDC submission pipeline (MX-273).
 *
 * Two CDC operations exist and must not be confused:
 *   Part 4 (Phase 1, ICdcScorePullService) — RCC credit report PULL.
 *     "Give me this client's current credit report."
 *   Part 1+2 (this interface) — Reportar Cartera en Línea SUBMISSION.
 *     "Here is this client's loan activity, record it."
 * submitClient() never calls ICdcScorePullService — different CDC endpoint,
 * different payload, different purpose. The KYC gate is shared logic
 * (IKycScoringService), reused directly, not via ICdcScorePullService.
 *
 * Mock mode (mifos.cdc.mock.enabled=true — same flag as ICdcScorePullService):
 *   submitClient() returns a synthetic ACCEPTED SubmissionRecord with a
 *   "MOCK-" prefixed cdcReferenceId. No external HTTP call.
 *
 * Real mode (Phase 2 — pending Victor's CDC credentials):
 *   Will call plugin POST /circulo-de-credito/rcc/{clientId} (PR #122,
 *   merged). Until then, throws CdcNotConfiguredException — same pattern
 *   as ICdcScorePullService.pullAndSave() in real mode.
 *
 * Used by:
 *   POST /api/submissions/run        — submitClient() or runBatch()
 *   POST /api/submissions/report-approval  (MX-276, Trigger 2) — submitClient()
 *   POST /api/submissions/report-screening (MX-276, Trigger 3) — submitClient()
 *   SubmissionRetryScheduler (MX-274) — re-invokes the CDC call portion
 *     for PENDING_RETRY records (see retrySubmission()).
 */
public interface ISubmissionService {

    /**
     * Submits a single client's loan activity to CDC.
     *
     * Flow:
     *   1. Validate clientId not null
     *   2. fineractApiClient.getClientData(clientId)
     *   3. kycScoringService.score(clientData)
     *   4. score < 70 (not ready) -> save SubmissionRecord(REJECTED,
     *      rejectionReason=missing fields), return — CDC never called
     *   5. score >= 70 -> mock or real CDC submission call
     *   6. Build + save SubmissionRecord with result
     *
     * triggerType determines which trigger created this record:
     *   LOAN_APPROVAL   — Trigger 2 (MX-276 report-approval)
     *   SCREENING_EVENT — Trigger 3 (MX-276 report-screening), requires inquiryType
     *   MANUAL_BATCH    — POST /api/submissions/run or runBatch()
     *
     * @param clientId    Fineract client ID — must not be null
     * @param triggerType how this submission was triggered — must not be null
     * @param loanId      related Fineract loan ID, nullable (null for
     *                     standalone screenings not tied to a loan)
     * @param inquiryType "HARD" or "SOFT" — required when triggerType is
     *                     SCREENING_EVENT, null otherwise (LRSIC inquiry
     *                     logging rule)
     * @return the saved SubmissionRecord, in whatever status resulted
     *          (ACCEPTED, REJECTED, PENDING_RETRY — never PARTIAL or
     *          PERMANENTLY_FAILED for a single-client call)
     * @throws IllegalArgumentException if clientId or triggerType is null,
     *          or if triggerType is SCREENING_EVENT and inquiryType is null
     */
    SubmissionRecord submitClient(
            Long clientId,
            TriggerType triggerType,
            Long loanId,
            String inquiryType);

    /**
     * Processes a batch of clients via submitClient(), each with
     * triggerType = MANUAL_BATCH, loanId = null, inquiryType = null.
     *
     * If clientIds is null or empty, processes every active client from
     * fineractApiClient.getAllActiveClientIds().
     *
     * @Async — the controller (POST /api/submissions/run) returns HTTP 202
     * immediately; this method runs in the background. Requires
     * @EnableAsync on CbIldApplication.
     *
     * Each client is processed independently — one client's failure
     * (Fineract 404, KYC gate, CDC error) does not stop the batch. The
     * returned list contains one SubmissionRecord per processed client,
     * in whatever status submitClient() produced for that client.
     *
     * @param clientIds specific clients to process, or null/empty for all
     *                    active clients
     * @return future resolving to one SubmissionRecord per processed client
     */
    CompletableFuture<List<SubmissionRecord>> runBatch(List<Long> clientIds);

    /**
     * Re-attempts the CDC submission call for an existing PENDING_RETRY
     * SubmissionRecord, without re-running the KYC gate or re-fetching
     * Fineract data — the original triggerType, loanId, and inquiryType
     * are preserved from the existing record.
     *
     * Used by:
     *   SubmissionRetryScheduler (MX-274) — @Scheduled every 6 hours.
     *
     * On success: status -> ACCEPTED, cdcReferenceId set.
     * On failure: retryCount incremented; if retryCount reaches
     *   SubmissionRetryProperties.maxAttempts, status -> PERMANENTLY_FAILED,
     *   otherwise status stays PENDING_RETRY and nextRetryAt is
     *   recalculated via exponential backoff
     *   (retryCount^2 * retryIntervalMinutes).
     *
     * @param existing a SubmissionRecord currently in PENDING_RETRY status
     * @return the same record, updated and saved with the new status
     * @throws IllegalArgumentException if existing is null or not in
     *          PENDING_RETRY status
     */
    SubmissionRecord retrySubmission(SubmissionRecord existing);
}
