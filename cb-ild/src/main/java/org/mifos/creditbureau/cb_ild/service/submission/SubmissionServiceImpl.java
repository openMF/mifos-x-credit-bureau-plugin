package org.mifos.creditbureau.cb_ild.service.submission;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.aop.Auditable;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;
import org.mifos.creditbureau.cb_ild.client.CdcPluginClient;
import org.mifos.creditbureau.cb_ild.exception.CdcNotConfiguredException;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.mifos.creditbureau.cb_ild.service.kyc.IKycScoringService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of ISubmissionService.
 *
 * Mock mode (mifos.cdc.mock.enabled=true — same flag as CdcScorePullServiceImpl):
 *   submitClient() and retrySubmission() return ACCEPTED with a
 *   "MOCK-" prefixed cdcReferenceId. No external HTTP calls made.
 *
 * Real mode (Phase 2 — pending Victor's CDC credentials):
 *   Will call plugin POST /circulo-de-credito/rcc/{clientId}. Until then,
 *   throws CdcNotConfiguredException — same pattern as
 *   CdcScorePullServiceImpl in real mode.
 *
 * Retry / backoff (MX-274, SubmissionRetryProperties):
 *   nextRetryAt = now + (retryCount^2 * retryIntervalMinutes)
 *   retryCount 1 -> 60min, 2 -> 240min, 3 -> 540min then PERMANENTLY_FAILED
 *
 * updatedAt:
 *   SubmissionRecord.updatedAt is NOT @UpdateTimestamp (deliberate — see
 *   entity Javadoc). Every save path in this class sets it explicitly to
 *   LocalDateTime.now() before repository.save(), so it always reflects
 *   the last status transition.
 *
 * Security:
 *   Never logs RFC value, FICO score, or cdcReferenceId — clientId and
 *   status only. The "score" logged in submitClient() is the KYC
 *   completeness score (0-100), not a credit/FICO score — distinct from
 *   the SEC-01 prohibition on logging ficoScore.
 */
@Slf4j
@Service
public class SubmissionServiceImpl implements ISubmissionService {

    private final SubmissionRecordRepository submissionRecordRepository;
    private final FineractApiClient fineractApiClient;
    private final IKycScoringService kycScoringService;
    private final SubmissionRetryProperties retryProperties;
    private final boolean mockEnabled;
    private final CdcPluginClient cdcPluginClient;

    /**
     * Self-proxy injection — required so runBatch() calls submitClient()
     * through the Spring AOP proxy, ensuring @Auditable fires correctly.
     * @Lazy breaks the circular dependency that would otherwise occur.
     */
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private SubmissionServiceImpl self;

    // Constructor injection — never @Autowired on fields.
    // Same mifos.cdc.mock.enabled property as CdcScorePullServiceImpl — one
    // flag controls both the Part 4 score pull and the Part 1+2 submission
    // pipeline, since both ultimately depend on the same plugin/CDC creds.
    public SubmissionServiceImpl(
            SubmissionRecordRepository submissionRecordRepository,
            FineractApiClient fineractApiClient,
            IKycScoringService kycScoringService,
            SubmissionRetryProperties retryProperties,
            @Value("${mifos.cdc.mock.enabled:true}") boolean mockEnabled,
            CdcPluginClient cdcPluginClient) {
        this.submissionRecordRepository = submissionRecordRepository;
        this.fineractApiClient = fineractApiClient;
        this.kycScoringService = kycScoringService;
        this.retryProperties = retryProperties;
        this.mockEnabled = mockEnabled;
        this.cdcPluginClient = cdcPluginClient;
        log.info("SubmissionServiceImpl initialized — mockEnabled: {}, maxAttempts: {}, retryIntervalMinutes: {}",
                mockEnabled, retryProperties.getMaxAttempts(), retryProperties.getRetryIntervalMinutes());
    }

    /**
     * Submits a single client's loan activity to CDC.
     *
     * Step 1: Validate clientId, triggerType, inquiryType
     * Step 2: Fetch Fineract client data (3 endpoints)
     * Step 3: KYC score
     * Step 4: score < 70 -> REJECTED, return — CDC never called
     * Step 5: mock or real CDC submission call
     * Step 6: Build + save SubmissionRecord
     */
    @Override
    @Auditable(action = "SUBMISSION_RUN", entityType = "SubmissionRecord")
    @Transactional
    public SubmissionRecord submitClient(
            Long clientId,
            TriggerType triggerType,
            Long loanId,
            String inquiryType) {

        validateSubmitClientArgs(clientId, triggerType, inquiryType);

        log.info("Starting submission — clientId: {}, triggerType: {}",
                clientId, triggerType);

        // Step 2 — Fineract client data
        FineractClientData clientData = fineractApiClient.getClientData(clientId);

        // Step 3 — KYC score
        KycReadinessResult kycResult = kycScoringService.score(clientData);

        log.debug("KYC result for submission — clientId: {}, score: {}, ready: {}",
                clientId, kycResult.score(), kycResult.ready());

        // Step 4 — KYC gate: score < 70 -> REJECTED, CDC never called
        if (!kycResult.ready()) {
            log.info("KYC gate blocked submission — clientId: {}, score: {} — CDC not called",
                    clientId, kycResult.score());
            return saveRejected(clientId, triggerType, loanId, inquiryType, kycResult);
        }

        // Step 5 — CDC submission call (mock or real)
        CdcSubmissionResult cdcResult = callCdcSubmission(clientId);

        // Step 6 — build + save
        return saveSubmissionResult(clientId, triggerType, loanId, inquiryType, cdcResult);
    }

    /**
     * Validates submitClient() arguments. Extracted to keep submitClient()
     * under the 40-line method limit (RULE 11).
     */
    private void validateSubmitClientArgs(Long clientId, TriggerType triggerType, String inquiryType) {
        if (clientId == null) {
            throw new IllegalArgumentException("clientId must not be null");
        }
        if (triggerType == null) {
            throw new IllegalArgumentException("triggerType must not be null");
        }
        if (triggerType == TriggerType.SCREENING_EVENT
                && (inquiryType == null || inquiryType.isBlank())) {
            throw new IllegalArgumentException(
                    "inquiryType is required when triggerType is SCREENING_EVENT");
        }
    }

    /**
     * Processes a batch of clients via submitClient(), each with
     * triggerType = MANUAL_BATCH, loanId = null, inquiryType = null.
     *
     * If clientIds is null or empty, processes every active client from
     * fineractApiClient.getAllActiveClientIds().
     *
     * Each client is processed independently — a single client's failure
     * is logged and that client is skipped; it does not stop the batch
     * and does not fail the overall CompletableFuture.
     *
     * Requires @EnableAsync on CbIldApplication, or this runs
     * synchronously and POST /api/submissions/run will block for the
     * duration of the entire batch (PE-01: up to ~90s per client x N
     * clients with sequential Fineract calls).
     */
    @Override
    @Async
    public CompletableFuture<List<SubmissionRecord>> runBatch(List<Long> clientIds) {

        List<Long> targets = (clientIds == null || clientIds.isEmpty())
                ? fineractApiClient.getAllActiveClientIds()
                : clientIds;

        log.info("Starting batch submission run — {} client(s)", targets.size());

        List<SubmissionRecord> results = new ArrayList<>();

        for (Long clientId : targets) {
            try {
                SubmissionRecord result = self.submitClient(
                        clientId, TriggerType.MANUAL_BATCH, null, null);
                results.add(result);
            } catch (Exception e) {
                // One client's failure does not stop the batch. Most
                // failures (Fineract 404/5xx/timeout) are environmental;
                // this catch guards against unexpected per-client errors
                // (e.g. a client deleted between list and fetch).
                log.error("Batch submission failed for clientId: {} — skipping. Reason: {}",
                        clientId, e.getMessage());
            }
        }

        log.info("Batch submission run complete — {} of {} client(s) processed successfully",
                results.size(), targets.size());

        return CompletableFuture.completedFuture(results);
    }

    /**
     * Re-attempts the CDC submission call for an existing PENDING_RETRY
     * SubmissionRecord. Does not re-run the KYC gate or re-fetch Fineract
     * data — triggerType, loanId, inquiryType are preserved from `existing`.
     */
    @Override
    @Auditable(action = "SUBMISSION_RETRY", entityType = "SubmissionRecord")
    @Transactional
    public SubmissionRecord retrySubmission(SubmissionRecord existing) {

        if (existing == null) {
            throw new IllegalArgumentException("existing must not be null");
        }
        if (existing.getStatus() != SubmissionStatus.PENDING_RETRY) {
            throw new IllegalArgumentException(
                    "existing must be in PENDING_RETRY status, was: " + existing.getStatus());
        }

        log.info("Retrying submission — clientId: {}, retryCount: {}",
                existing.getClientId(), existing.getRetryCount());

        CdcSubmissionResult cdcResult = callCdcSubmission(existing.getClientId());

        if (cdcResult.accepted()) {
            applyRetrySuccess(existing, cdcResult);
        } else {
            applyRetryFailure(existing, cdcResult);
        }

        return submissionRecordRepository.save(existing);
    }

    /**
     * Updates `existing` in place for a successful retry. Extracted to
     * keep retrySubmission() under the 40-line method limit (RULE 11).
     */
    private void applyRetrySuccess(SubmissionRecord existing, CdcSubmissionResult cdcResult) {
        existing.setStatus(SubmissionStatus.ACCEPTED);
        existing.setCdcReferenceId(cdcResult.cdcReferenceId());
        existing.setRejectionReason(null);
        existing.setSubmittedAt(LocalDateTime.now());
        existing.setUpdatedAt(LocalDateTime.now());

        log.info("Retry succeeded — clientId: {}, status: ACCEPTED",
                existing.getClientId());
    }

    /**
     * Updates `existing` in place for a failed retry — increments
     * retryCount, either escalates to PERMANENTLY_FAILED or schedules the
     * next retry via exponential backoff. Extracted to keep
     * retrySubmission() under the 40-line method limit (RULE 11).
     */
    private void applyRetryFailure(SubmissionRecord existing, CdcSubmissionResult cdcResult) {
        int newRetryCount = existing.getRetryCount() + 1;
        existing.setRetryCount(newRetryCount);
        existing.setRejectionReason(cdcResult.failureReason());
        existing.setUpdatedAt(LocalDateTime.now());

        if (newRetryCount >= retryProperties.getMaxAttempts()) {
            existing.setStatus(SubmissionStatus.PERMANENTLY_FAILED);
            existing.setNextRetryAt(null);

            log.warn("Retry exhausted — clientId: {}, retryCount: {} — status: PERMANENTLY_FAILED",
                    existing.getClientId(), newRetryCount);
        } else {
            existing.setStatus(SubmissionStatus.PENDING_RETRY);
            existing.setNextRetryAt(LocalDateTime.now().plusMinutes(computeBackoffMinutes(newRetryCount)));

            log.info("Retry failed — clientId: {}, retryCount: {} — status: PENDING_RETRY, nextRetryAt: {}",
                    existing.getClientId(), newRetryCount, existing.getNextRetryAt());
        }
    }

    /**
     * Computes exponential backoff delay in minutes.
     *
     * nextRetryAt = now + (retryCount^2 * retryIntervalMinutes)
     *
     * With default retryIntervalMinutes=60:
     *   retryCount 1 -> 60min  (~1hr)
     *   retryCount 2 -> 240min (~4hr)
     *   retryCount 3 -> 540min (~9hr) — then PERMANENTLY_FAILED, this
     *                  value is never used as retryCount >= maxAttempts
     *
     * @param retryCount the new retry count (after incrementing), >= 1
     * @return delay in minutes
     */
    private long computeBackoffMinutes(int retryCount) {
        return (long) retryCount * retryCount * retryProperties.getRetryIntervalMinutes();
    }

    /**
     * Performs the CDC submission call — mock or real.
     *
     * Mock mode: always returns accepted=true with a "MOCK-" prefixed
     * cdcReferenceId. No external call.
     *
     * Real mode: not yet implemented — throws CdcNotConfiguredException,
     * same as CdcScorePullServiceImpl.pullAndSave() in real mode. When
     * Victor's CDC credentials arrive, this branch will call plugin
     * POST /circulo-de-credito/rcc/{clientId} (PR #122, merged) and map
     * its response (ACCEPTED/REJECTED + cdcReferenceId, or error +
     * failureReason) to CdcSubmissionResult.
     */
    private CdcSubmissionResult callCdcSubmission(Long clientId) {
        if (mockEnabled) {
            String mockReferenceId = "MOCK-" + UUID.randomUUID();
            log.debug("Mock CDC submission — clientId: {}, cdcReferenceId assigned", clientId);
            return new CdcSubmissionResult(true, mockReferenceId, null);
        }

        // Phase 2 — real CDC call via plugin (MX-276)
        // Re-throw known exceptions so GlobalExceptionHandler returns
        // correct HTTP status (503 for CdcNotConfiguredException,
        // 503 for CdcTimeoutException, 400 for CdcBadRequestException).
        // Only catch truly unexpected exceptions as PENDING_RETRY.
        try {
            java.util.Map<String, Object> report =
                    cdcPluginClient.fetchCreditReport(clientId);
            String folioConsulta = report.get("reportId") != null
                    ? report.get("reportId").toString()
                    : "CDC-" + java.util.UUID.randomUUID();
            log.info("CDC submission accepted — clientId: {}", clientId);
            return new CdcSubmissionResult(true, folioConsulta, null);
        } catch (org.mifos.creditbureau.cb_ild.exception.CdcNotConfiguredException e) {
            throw e; // 503 — configuration error, not retryable, re-throw to controller
        } catch (org.mifos.creditbureau.cb_ild.exception.CdcTimeoutException e) {
            // Retryable — return failure result so PENDING_RETRY record is saved
            log.warn("CDC timeout for clientId: {} — scheduling PENDING_RETRY", clientId);
            return new CdcSubmissionResult(false, null,
                    "CDC timeout: " + e.getMessage());
        } catch (org.mifos.creditbureau.cb_ild.exception.CdcServerException e) {
            // Retryable — return failure result so PENDING_RETRY record is saved
            log.warn("CDC server error for clientId: {} — scheduling PENDING_RETRY", clientId);
            return new CdcSubmissionResult(false, null,
                    "CDC server error: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected CDC error — clientId: {}, error: {}",
                    clientId, e.getMessage());
            return new CdcSubmissionResult(false, null,
                    "Unexpected CDC error: " + e.getClass().getSimpleName());
        }
    }

    /**
     * Builds and saves a REJECTED SubmissionRecord when the KYC gate
     * blocks the client. CDC was never called.
     *
     * rejectionReason summarizes the KYC score and missing fields —
     * field names only (e.g. "dateOfBirth", "address"), never raw PII
     * values, matching KycCompletenessScorer's missingFields convention.
     */
    private SubmissionRecord saveRejected(
            Long clientId,
            TriggerType triggerType,
            Long loanId,
            String inquiryType,
            KycReadinessResult kycResult) {

        String rejectionReason = String.format(
                "KYC score %d below threshold — missing fields: %s",
                kycResult.score(), kycResult.missingFields());

        LocalDateTime now = LocalDateTime.now();

        SubmissionRecord record = SubmissionRecord.builder()
                .clientId(clientId)
                .loanId(loanId)
                .triggerType(triggerType)
                .status(SubmissionStatus.REJECTED)
                .rejectionReason(rejectionReason)
                .retryCount(0)
                .submittedAt(null)
                .updatedAt(now)
                .inquiryType(inquiryType)
                .expiryDate(LocalDate.now().plusMonths(72))
                .build();

        return submissionRecordRepository.save(record);
    }

    /**
     * Builds and saves a SubmissionRecord reflecting the CDC submission
     * result. accepted=true -> ACCEPTED; accepted=false -> PENDING_RETRY
     * (first attempt, retryCount=0, nextRetryAt computed with retryCount=1
     * since the next attempt will be retry #1).
     *
     * A submission is never saved as PARTIAL or PERMANENTLY_FAILED on the
     * first attempt — PARTIAL applies to batch-level summaries (not yet
     * surfaced per-record here) and PERMANENTLY_FAILED only occurs after
     * retryProperties.maxAttempts via retrySubmission().
     */
    private SubmissionRecord saveSubmissionResult(
            Long clientId,
            TriggerType triggerType,
            Long loanId,
            String inquiryType,
            CdcSubmissionResult cdcResult) {

        LocalDateTime now = LocalDateTime.now();

        SubmissionRecord.SubmissionRecordBuilder builder = SubmissionRecord.builder()
                .clientId(clientId)
                .loanId(loanId)
                .triggerType(triggerType)
                .submittedAt(now)
                .updatedAt(now)
                .inquiryType(inquiryType)
                .retryCount(0)
                .expiryDate(LocalDate.now().plusMonths(72));

        if (cdcResult.accepted()) {
            builder.status(SubmissionStatus.ACCEPTED)
                    .cdcReferenceId(cdcResult.cdcReferenceId());

            log.info("Submission ACCEPTED — clientId: {}", clientId);

        } else {
            builder.status(SubmissionStatus.PENDING_RETRY)
                    .rejectionReason(cdcResult.failureReason())
                    .nextRetryAt(now.plusMinutes(computeBackoffMinutes(1)));

            log.info("Submission PENDING_RETRY — clientId: {}, nextRetryAt: {}",
                    clientId, now.plusMinutes(computeBackoffMinutes(1)));
        }

        return submissionRecordRepository.save(builder.build());
    }

    /**
     * Result of a CDC submission call — mock or real.
     *
     * accepted=true:  cdcReferenceId set, failureReason null
     * accepted=false: cdcReferenceId null, failureReason set
     */
    private record CdcSubmissionResult(
            boolean accepted,
            String cdcReferenceId,
            String failureReason) {}
}
