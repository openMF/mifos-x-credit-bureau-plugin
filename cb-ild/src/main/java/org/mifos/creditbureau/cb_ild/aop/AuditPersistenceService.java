package org.mifos.creditbureau.cb_ild.aop;
import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.entity.AuditEntry;
import org.mifos.creditbureau.cb_ild.repository.AuditEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separate @Service bean for saving audit entries.
 *
 * CRITICAL — must be a separate bean from CbildAuditAspect.
 * Calls from CbildAuditAspect go through Spring proxy so
 * @Transactional(REQUIRES_NEW) fires correctly.
 *
 * Compliance guarantee:
 *   Audit entry saved in its own independent transaction.
 *   Never rolled back even if caller's transaction fails.
 */
@Slf4j
@Service
public class AuditPersistenceService {
    private final AuditEntryRepository auditEntryRepository;

    public AuditPersistenceService(AuditEntryRepository auditEntryRepository) {
        this.auditEntryRepository = auditEntryRepository;
    }

    /**
     * Saves audit entry without clientId (backward compatible).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditEntry(
            String action,
            String entityType,
            String userId,
            String requestId,
            long durationMs,
            String result,
            String errorMessage) {
        saveAuditEntry(action, entityType, userId, requestId,
                durationMs, result, errorMessage, null);
    }

    /**
     * Saves audit entry with clientId — used when clientId is known.
     * clientId enables GET /api/clients/{id}/audit-trail queries.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveAuditEntry(
            String action,
            String entityType,
            String userId,
            String requestId,
            long durationMs,
            String result,
            String errorMessage,
            Long clientId) {
        AuditEntry entry = AuditEntry.builder()
                .action(action)
                .entityType(entityType)
                .performedBy(userId)
                .requestId(requestId)
                .durationMs(durationMs)
                .result(result)
                .errorMessage(errorMessage)
                .clientId(clientId)
                .build();
        auditEntryRepository.save(entry);
        log.debug("Audit saved — action: {}, result: {}, duration: {}ms, clientId: {}",
                action, result, durationMs, clientId);
    }
}
