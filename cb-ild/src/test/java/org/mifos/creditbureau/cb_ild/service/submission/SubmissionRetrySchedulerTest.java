package org.mifos.creditbureau.cb_ild.service.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SubmissionRetryScheduler.
 *
 * Follows the exact same pattern as RetentionServiceTest:
 *   @ExtendWith(MockitoExtension.class), constructor injection in setUp(),
 *   private fixture methods, @DisplayName on every test.
 *
 * Key difference from RetentionServiceTest:
 *   RetentionService uses findAll (List) — we use paginated query (Page).
 *   Tests mock Page<SubmissionRecord> using PageImpl.
 *   hasNext()=false means single page (no pagination loop needed for tests).
 *
 * Test 1: empty page - retrySubmission never called
 * Test 2: one record due - retrySubmission called once
 * Test 3: multiple records due - retrySubmission called for each
 * Test 4: one record throws - continues to next, does not throw
 * Test 5: all records fail - logs errors, does not throw
 */
@ExtendWith(MockitoExtension.class)
class SubmissionRetrySchedulerTest {

    @Mock
    private SubmissionRecordRepository submissionRecordRepository;

    @Mock
    private ISubmissionService submissionService;

    @Mock
    private SubmissionRetryProperties retryProperties;

    private SubmissionRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        when(retryProperties.getPageSize()).thenReturn(100);
        scheduler = new SubmissionRetryScheduler(
                submissionRecordRepository,
                submissionService,
                retryProperties);
    }

    private SubmissionRecord pendingRetryRecord(Long id) {
        return SubmissionRecord.builder()
                .id(id)
                .clientId(id)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.PENDING_RETRY)
                .retryCount(1)
                .nextRetryAt(LocalDateTime.now().minusHours(1))
                .updatedAt(LocalDateTime.now().minusHours(1))
                .build();
    }

    private Page<SubmissionRecord> emptyPage() {
        return new PageImpl<>(Collections.emptyList());
    }

    private Page<SubmissionRecord> singlePage(List<SubmissionRecord> records) {
        return new PageImpl<>(records);
    }

    @Test
    @DisplayName("empty page - retrySubmission never called")
    void processRetryQueue_emptyPage_retryNeverCalled() {
        when(submissionRecordRepository.findByStatusAndNextRetryAtBefore(
                eq(SubmissionStatus.PENDING_RETRY),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(emptyPage());

        scheduler.processRetryQueue();

        verify(submissionService, never()).retrySubmission(any());
    }

    @Test
    @DisplayName("one record due - retrySubmission called once")
    void processRetryQueue_oneRecord_retryCalledOnce() {
        SubmissionRecord record = pendingRetryRecord(1L);

        when(submissionRecordRepository.findByStatusAndNextRetryAtBefore(
                eq(SubmissionStatus.PENDING_RETRY),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(singlePage(List.of(record)));

        scheduler.processRetryQueue();

        verify(submissionService, times(1)).retrySubmission(record);
    }

    @Test
    @DisplayName("multiple records due - retrySubmission called for each")
    void processRetryQueue_multipleRecords_retryCalledForEach() {
        List<SubmissionRecord> records = List.of(
                pendingRetryRecord(1L),
                pendingRetryRecord(2L),
                pendingRetryRecord(3L)
        );

        when(submissionRecordRepository.findByStatusAndNextRetryAtBefore(
                eq(SubmissionStatus.PENDING_RETRY),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(singlePage(records));

        scheduler.processRetryQueue();

        verify(submissionService, times(3)).retrySubmission(any());
    }

    @Test
    @DisplayName("one record throws - continues to next, does not throw")
    void processRetryQueue_oneRecordThrows_continuesAndDoesNotThrow() {
        SubmissionRecord record1 = pendingRetryRecord(1L);
        SubmissionRecord record2 = pendingRetryRecord(2L);

        when(submissionRecordRepository.findByStatusAndNextRetryAtBefore(
                eq(SubmissionStatus.PENDING_RETRY),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(singlePage(List.of(record1, record2)));

        doThrow(new RuntimeException("CDC timeout"))
                .when(submissionService).retrySubmission(record1);

        // Should not throw - continues to record2
        scheduler.processRetryQueue();

        verify(submissionService, times(1)).retrySubmission(record2);
    }

    @Test
    @DisplayName("all records fail - logs errors, does not throw")
    void processRetryQueue_allRecordsFail_doesNotThrow() {
        SubmissionRecord record = pendingRetryRecord(1L);

        when(submissionRecordRepository.findByStatusAndNextRetryAtBefore(
                eq(SubmissionStatus.PENDING_RETRY),
                any(LocalDateTime.class),
                any(Pageable.class)))
                .thenReturn(singlePage(List.of(record)));

        doThrow(new RuntimeException("CDC timeout"))
                .when(submissionService).retrySubmission(any());

        // Should not throw even if all records fail
        scheduler.processRetryQueue();

        verify(submissionService, times(1)).retrySubmission(any());
    }
}
