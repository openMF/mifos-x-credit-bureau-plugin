package org.mifos.creditbureau.cb_ild.service.submission;

import lombok.extern.slf4j.Slf4j;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Scheduled retry job for failed CDC submission attempts (MX-274).
 *
 * Runs every 6 hours. Finds all SubmissionRecord rows in PENDING_RETRY
 * status whose nextRetryAt has passed, and calls
 * ISubmissionService.retrySubmission() on each one.
 *
 * Why 6 hours:
 *   Backoff formula (retryCount^2 x retryIntervalMinutes, default 60min):
 *     retryCount=1 - nextRetryAt += 60min  (~1hr)
 *     retryCount=2 - nextRetryAt += 240min (~4hr)
 *   A 6-hour cadence means no retry is always due on every run.
 *   Each backoff window is meaningful relative to the schedule.
 *
 * Pagination (PE-02):
 *   Uses findByStatusAndNextRetryAtBefore with Pageable (pageSize=100).
 *   Never loads all PENDING_RETRY rows into heap at once.
 *   Processes one page at a time until no more pages remain.
 *
 * Per-row isolation (matches RetentionService pattern):
 *   Each record is processed in its own try/catch.
 *   One record failure does not stop the rest of the batch.
 *   retrySubmission() is @Transactional on SubmissionServiceImpl
 *   (a separate Spring bean) - proxy fires correctly, no self-invocation
 *   issue (unlike RetentionService which needed a separate archive bean).
 *
 * Security:
 *   Never logs clientId list - enumeration risk.
 *   Logs only counts and individual record IDs on error.
 *
 * @EnableScheduling is already on CbIldApplication.
 */
@Slf4j
@Component
public class SubmissionRetryScheduler {

    private final SubmissionRecordRepository submissionRecordRepository;
    private final ISubmissionService submissionService;
    private final SubmissionRetryProperties retryProperties;

    public SubmissionRetryScheduler(
            SubmissionRecordRepository submissionRecordRepository,
            ISubmissionService submissionService,
            SubmissionRetryProperties retryProperties) {
        this.submissionRecordRepository = submissionRecordRepository;
        this.submissionService = submissionService;
        this.retryProperties = retryProperties;
    }

    /**
     * Retry queue processor - runs every 6 hours.
     * cron = "0 0 (star)/6 * * *" - every 6th hour, every day.
     *
     * Finds all PENDING_RETRY records whose nextRetryAt <= now,
     * processes them page by page, calls retrySubmission() on each.
     */
    @Scheduled(cron = "0 0 */6 * * *")
    public void processRetryQueue() {
        log.info("SubmissionRetryScheduler - starting retry queue run");

        LocalDateTime now = LocalDateTime.now();
        int page = 0;
        int totalProcessed = 0;
        int totalFailed = 0;

        Page<SubmissionRecord> batch;

        do {
            batch = submissionRecordRepository
                    .findByStatusAndNextRetryAtBefore(
                            SubmissionStatus.PENDING_RETRY,
                            now,
                            PageRequest.of(page, retryProperties.getPageSize())
                    );

            if (batch.isEmpty()) {
                break;
            }

            log.info("SubmissionRetryScheduler - processing page {} ({} records)",
                    page, batch.getNumberOfElements());

            for (SubmissionRecord record : batch) {
                try {
                    submissionService.retrySubmission(record);
                    totalProcessed++;
                } catch (Exception e) {
                    // One record failure does not stop the batch.
                    // Matches RetentionService per-row isolation pattern.
                    log.error("SubmissionRetryScheduler - failed to retry " +
                                    "record id: {} - {}",
                            record.getId(), e.getMessage());
                    totalFailed++;
                }
            }

            page++;

        } while (batch.hasNext());

        log.info("SubmissionRetryScheduler - complete. Processed: {}, Failed: {}",
                totalProcessed, totalFailed);
    }
}
