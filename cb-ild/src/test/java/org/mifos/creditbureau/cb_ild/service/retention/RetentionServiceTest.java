package org.mifos.creditbureau.cb_ild.service.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for RetentionService.
 *
 * Test 1: no expired rows — archiveService never called
 * Test 2: one expired row — archiveService called once
 * Test 3: multiple expired rows — archiveService called for each
 * Test 4: one row fails — continues to next, does not throw
 * Test 5: all rows fail — logs errors, does not throw
 */
@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    @Mock
    private BureauResponseRepository bureauResponseRepository;

    @Mock
    private RetentionArchiveService retentionArchiveService;

    private RetentionService retentionService;

    @BeforeEach
    void setUp() {
        retentionService = new RetentionService(
                bureauResponseRepository, retentionArchiveService);
    }

    private BureauResponseEntity expiredEntity(Long id) {
        return BureauResponseEntity.builder()
                .clientId(id)
                .ficoScore(750)
                .expiryDate(LocalDate.now().minusDays(1))
                .softDeleted(false)
                .build();
    }

    @Test
    @DisplayName("no expired rows — archiveService never called")
    void archiveExpiredRecords_noExpiredRows_archiveNeverCalled() {
        when(bureauResponseRepository.findAllByExpiryDateBefore(
                any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        retentionService.archiveExpiredRecords();

        verify(retentionArchiveService, never())
                .archiveSingleRecord(any());
    }

    @Test
    @DisplayName("one expired row — archiveService called once")
    void archiveExpiredRecords_oneExpiredRow_archiveCalledOnce() {
        BureauResponseEntity entity = expiredEntity(1L);
        when(bureauResponseRepository.findAllByExpiryDateBefore(
                any(LocalDate.class)))
                .thenReturn(List.of(entity));

        retentionService.archiveExpiredRecords();

        verify(retentionArchiveService, times(1))
                .archiveSingleRecord(entity);
    }

    @Test
    @DisplayName("multiple expired rows — archiveService called for each")
    void archiveExpiredRecords_multipleRows_archiveCalledForEach() {
        List<BureauResponseEntity> entities = List.of(
                expiredEntity(1L),
                expiredEntity(2L),
                expiredEntity(3L)
        );
        when(bureauResponseRepository.findAllByExpiryDateBefore(
                any(LocalDate.class)))
                .thenReturn(entities);

        retentionService.archiveExpiredRecords();

        verify(retentionArchiveService, times(3))
                .archiveSingleRecord(any());
    }

    @Test
    @DisplayName("one row fails — continues to next, does not throw")
    void archiveExpiredRecords_oneRowFails_continuesAndDoesNotThrow() {
        BureauResponseEntity entity1 = expiredEntity(1L);
        BureauResponseEntity entity2 = expiredEntity(2L);

        when(bureauResponseRepository.findAllByExpiryDateBefore(
                any(LocalDate.class)))
                .thenReturn(List.of(entity1, entity2));

        doThrow(new RuntimeException("DB error"))
                .when(retentionArchiveService)
                .archiveSingleRecord(entity1);

        // Should not throw — continues to entity2
        retentionService.archiveExpiredRecords();

        verify(retentionArchiveService, times(1))
                .archiveSingleRecord(entity2);
    }

    @Test
    @DisplayName("all rows fail — logs errors, does not throw")
    void archiveExpiredRecords_allRowsFail_doesNotThrow() {
        BureauResponseEntity entity = expiredEntity(1L);

        when(bureauResponseRepository.findAllByExpiryDateBefore(
                any(LocalDate.class)))
                .thenReturn(List.of(entity));

        doThrow(new RuntimeException("DB error"))
                .when(retentionArchiveService)
                .archiveSingleRecord(any());

        // Should not throw even if all rows fail
        retentionService.archiveExpiredRecords();

        verify(retentionArchiveService, times(1))
                .archiveSingleRecord(any());
    }
}
