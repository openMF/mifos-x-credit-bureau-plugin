package org.mifos.creditbureau.cb_ild.service.retention;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * LRSIC retention service — nightly archive job.
 *
 * LRSIC (Ley para Regular las Sociedades de Informacion Crediticia)
 * Mexican credit bureau law:
 *   Credit bureau data expires 72 months after dateOfFirstDelinquency.
 *   Expired data must be archived — never hard-deleted.
 *
 * Schedule: 2am every night
 *   cron = "0 0 2 * * *"
 *   second=0, minute=0, hour=2, every day
 *
 * @EnableScheduling is in CbIldApplication — already active.
 *
 * Why delegates to RetentionArchiveService:
 *   Self-invocation proxy issue — same as AuditPersistenceService.
 *   Each row needs its own @Transactional via separate bean.
 *   Failure of one row does not affect other rows.
 *
 * Security:
 *   Never log clientId list — enumeration risk
 *   Never log FICO values
 *   Log only counts — "Archived N records"
 */
@Slf4j
@Service
public class RetentionService implements IRetentionService {

    private final BureauResponseRepository bureauResponseRepository;
    private final RetentionArchiveService retentionArchiveService;

    public RetentionService(
            BureauResponseRepository bureauResponseRepository,
            RetentionArchiveService retentionArchiveService) {
        this.bureauResponseRepository = bureauResponseRepository;
        this.retentionArchiveService = retentionArchiveService;
    }

    /**
     * Nightly archive job — runs at 2am every day.
     * cron format: second minute hour day month weekday
     * "0 0 2 * * *" = at 00s, 00m, 02h, every day
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Override
    public void archiveExpiredRecords() {
        log.info("RetentionService — starting nightly archive job");

        LocalDate today = LocalDate.now();

        // @SQLRestriction adds soft_deleted=false automatically
        // JPA excludes null expiryDate rows automatically
        List<BureauResponseEntity> expiredRows =
                bureauResponseRepository.findAllByExpiryDateBefore(today);

        if (expiredRows.isEmpty()) {
            log.info("RetentionService — no expired records found");
            return;
        }

        log.info("RetentionService — found {} expired records",
                expiredRows.size());

        int archived = 0;
        int failed = 0;

        for (BureauResponseEntity entity : expiredRows) {
            try {
                // Separate @Service bean — @Transactional fires correctly
                retentionArchiveService.archiveSingleRecord(entity);
                archived++;
            } catch (Exception e) {
                // Never fail entire batch for one bad row
                log.error("RetentionService — failed to archive " +
                        "record id: {} — {}",
                        entity.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("RetentionService — complete. Archived: {}, Failed: {}",
                archived, failed);
    }
}
