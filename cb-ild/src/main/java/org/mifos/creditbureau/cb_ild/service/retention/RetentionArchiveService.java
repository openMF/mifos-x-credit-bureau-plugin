package org.mifos.creditbureau.cb_ild.service.retention;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseArchive;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseArchiveRepository;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Separate @Service bean for archiving one bureau_response row.
 *
 * CRITICAL — must be separate from RetentionService.
 *
 * Why separate bean:
 *   Spring @Transactional works via proxy.
 *   RetentionService calling this.archiveSingleRecord() would
 *   bypass the proxy — same self-invocation issue as AuditPersistenceService.
 *   Separate bean ensures @Transactional fires correctly per row.
 *
 * Why per-row transaction:
 *   Each row is independent.
 *   If one row fails, the rest continue.
 *   Never fail entire batch for one bad row.
 *
 * Compliance:
 *   Archive row saved BEFORE soft-delete.
 *   If archive save fails, original NOT soft-deleted.
 *   No data loss under any failure scenario.
 */
@Slf4j
@Service
public class RetentionArchiveService {

    private final BureauResponseArchiveRepository archiveRepository;
    private final BureauResponseRepository bureauResponseRepository;

    public RetentionArchiveService(
            BureauResponseArchiveRepository archiveRepository,
            BureauResponseRepository bureauResponseRepository) {
        this.archiveRepository = archiveRepository;
        this.bureauResponseRepository = bureauResponseRepository;
    }

    /**
     * Archive one bureau_response row.
     *
     * Steps:
     *   1. Build archive summary — no PII, no fullResponse
     *   2. Save to bureau_response_archive
     *   3. Set softDeleted=true on original
     *   4. Save original row
     *
     * @Transactional — archive save + soft-delete atomic per row.
     * If archive save fails → rollback → original NOT soft-deleted.
     *
     * Security:
     *   fullResponse NEVER copied — raw CDC PII
     *   Only summary: clientId, ficoScore, pulledAt
     *   archivedAt auto-set by @CreationTimestamp
     *   Never logs FICO value or clientId
     */
    @Transactional
    public void archiveSingleRecord(BureauResponseEntity entity) {

        // Step 1 — build archive summary — no PII
        BureauResponseArchive archive = BureauResponseArchive.builder()
                .clientId(entity.getClientId())
                .ficoScore(entity.getFicoScore())
                .pulledAt(entity.getPulledAt())
                .build();

        // Step 2 — save archive first
        // If this fails — @Transactional rolls back
        // original NOT soft-deleted — no data loss
        archiveRepository.save(archive);

        // Step 3 + 4 — soft-delete original
        // NEVER hard-delete — LRSIC compliance
        entity.setSoftDeleted(true);
        bureauResponseRepository.save(entity);

        log.debug("Archived record — originalId: {}", entity.getId());
    }
}
