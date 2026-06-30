package org.mifos.creditbureau.cb_ild.service.dispute;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.aop.Auditable;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.entity.DisputeCase;
import org.mifos.creditbureau.cb_ild.entity.enums.DisputeStatus;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.mifos.creditbureau.cb_ild.repository.DisputeCaseRepository;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of IDisputeService (MX-275).
 *
 * institutionData snapshot: safe FineractClientData fields only.
 *   Excluded (PII): nationalId (RFC), dateOfBirth.
 *   Included: firstName, lastName, phoneNumber, emailAddress,
 *   addressLine1, addressLine2, addressLine3, city, state,
 *   postalCode, country.
 *
 * cdcData snapshot: safe BureauResponseEntity fields only.
 *   Excluded: fullResponse, rawResponseHash.
 *   Included: ficoScore, riskBand, hasDelinquencies,
 *   tradelines, alerts, pulledAt.
 *
 * Error handling:
 *   IllegalArgumentException -> 400 INVALID_ARGUMENT
 *   IllegalStateException    -> 400 INVALID_STATE_TRANSITION
 *   Both handled by GlobalExceptionHandler (added MX-275).
 */
@Slf4j
@Service
public class DisputeServiceImpl implements IDisputeService {

    private final DisputeCaseRepository disputeCaseRepository;
    private final SubmissionRecordRepository submissionRecordRepository;
    private final BureauResponseRepository bureauResponseRepository;
    private final FineractApiClient fineractApiClient;
    private final ObjectMapper objectMapper;

    public DisputeServiceImpl(
            DisputeCaseRepository disputeCaseRepository,
            SubmissionRecordRepository submissionRecordRepository,
            BureauResponseRepository bureauResponseRepository,
            FineractApiClient fineractApiClient,
            ObjectMapper objectMapper) {
        this.disputeCaseRepository = disputeCaseRepository;
        this.submissionRecordRepository = submissionRecordRepository;
        this.bureauResponseRepository = bureauResponseRepository;
        this.fineractApiClient = fineractApiClient;
        this.objectMapper = objectMapper;
    }

    @Override
    @Auditable(action = "DISPUTE_CREATE", entityType = "DisputeCase")
    @Transactional
    public DisputeCase createDispute(
            Long submissionRecordId,
            String disputeDetails,
            String raisedBy) {

        validateCreateArgs(submissionRecordId, disputeDetails, raisedBy);

        var submission = submissionRecordRepository.findById(submissionRecordId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "SubmissionRecord not found: " + submissionRecordId));

        Long clientId = submission.getClientId();
        log.info("Creating dispute - submissionRecordId: {}, clientId: {}",
                submissionRecordId, clientId);

        String institutionData = snapshotInstitutionData(clientId);
        String cdcData = snapshotCdcData(clientId);

        DisputeCase dispute = DisputeCase.builder()
                .submissionRecordId(submissionRecordId)
                .status(DisputeStatus.OPEN)
                .disputeDetails(disputeDetails)
                .institutionData(institutionData)
                .cdcData(cdcData)
                .raisedBy(raisedBy)
                .openedAt(LocalDateTime.now())
                .expiryDate(LocalDate.now().plusMonths(72))
                .build();

        DisputeCase saved = disputeCaseRepository.save(dispute);
        log.info("Dispute created - id: {}, status: OPEN", saved.getId());
        return saved;
    }

    @Override
    @Auditable(action = "DISPUTE_STATUS_UPDATE", entityType = "DisputeCase")
    @Transactional
    public DisputeCase updateStatus(
            Long disputeId,
            String newStatus,
            String resolutionNotes) {

        if (disputeId == null) {
            throw new IllegalArgumentException("disputeId must not be null");
        }
        if (newStatus == null || newStatus.isBlank()) {
            throw new IllegalArgumentException(
                    "newStatus must not be null or blank");
        }

        DisputeCase dispute = disputeCaseRepository.findById(disputeId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dispute not found: " + disputeId));

        DisputeStatus target = parseStatus(newStatus);
        validateTransition(dispute.getStatus(), target);

        dispute.setStatus(target);

        if (target == DisputeStatus.RESOLVED) {
            dispute.setResolvedAt(LocalDateTime.now());
            dispute.setResolutionNotes(resolutionNotes);
        }

        log.info("Dispute status updated - id: {}, newStatus: {}",
                disputeId, target);

        return disputeCaseRepository.save(dispute);
    }

    private void validateCreateArgs(
            Long submissionRecordId,
            String disputeDetails,
            String raisedBy) {
        if (submissionRecordId == null) {
            throw new IllegalArgumentException(
                    "submissionRecordId must not be null");
        }
        if (disputeDetails == null || disputeDetails.isBlank()) {
            throw new IllegalArgumentException(
                    "disputeDetails must not be null or blank");
        }
        if (raisedBy == null || raisedBy.isBlank()) {
            throw new IllegalArgumentException(
                    "raisedBy must not be null or blank");
        }
    }

    private void validateTransition(
            DisputeStatus current, DisputeStatus target) {
        boolean allowed =
                (current == DisputeStatus.OPEN
                        && target == DisputeStatus.UNDER_REVIEW)
                || (current == DisputeStatus.UNDER_REVIEW
                        && target == DisputeStatus.RESOLVED);
        if (!allowed) {
            throw new IllegalStateException(
                    "Invalid dispute transition: " + current + " -> " + target);
        }
    }

    private DisputeStatus parseStatus(String newStatus) {
        try {
            return DisputeStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid dispute status: " + newStatus
                    + ". Valid values: OPEN, UNDER_REVIEW, RESOLVED");
        }
    }

    private String snapshotInstitutionData(Long clientId) {
        try {
            FineractClientData data = fineractApiClient.getClientData(clientId);
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("clientId", clientId);
            safe.put("firstName", nullSafe(data.firstName()));
            safe.put("lastName", nullSafe(data.lastName()));
            safe.put("phoneNumber", nullSafe(data.phoneNumber()));
            safe.put("emailAddress", nullSafe(data.emailAddress()));
            safe.put("addressLine1", nullSafe(data.addressLine1()));
            safe.put("addressLine2", nullSafe(data.addressLine2()));
            safe.put("addressLine3", nullSafe(data.addressLine3()));
            safe.put("city", nullSafe(data.city()));
            safe.put("state", nullSafe(data.state()));
            safe.put("postalCode", nullSafe(data.postalCode()));
            safe.put("country", nullSafe(data.country()));
            return objectMapper.writeValueAsString(safe);
        } catch (Exception e) {
            log.warn("Could not snapshot institution data for clientId: {} - {}",
                    clientId, e.getMessage());
            return null;
        }
    }

    private String snapshotCdcData(Long clientId) {
        try {
            Optional<BureauResponseEntity> latest =
                    bureauResponseRepository
                            .findTopByClientIdOrderByPulledAtDesc(clientId);
            if (latest.isEmpty()) {
                log.debug("No bureau response for clientId: {} - cdcData null",
                        clientId);
                return null;
            }
            BureauResponseEntity entity = latest.get();
            Map<String, Object> safe = new LinkedHashMap<>();
            safe.put("ficoScore", nullSafe(entity.getFicoScore()));
            safe.put("riskBand", nullSafe(entity.getRiskBand()));
            safe.put("hasDelinquencies", nullSafe(entity.getHasDelinquencies()));
            safe.put("tradelines", nullSafe(entity.getTradelines()));
            safe.put("alerts", nullSafe(entity.getAlerts()));
            safe.put("pulledAt", nullSafe(entity.getPulledAt()));
            return objectMapper.writeValueAsString(safe);
        } catch (Exception e) {
            log.warn("Could not snapshot CDC data for clientId: {} - {}",
                    clientId, e.getMessage());
            return null;
        }
    }

    private Object nullSafe(Object value) {
        return value != null ? value : "";
    }
}
