package org.mifos.creditbureau.cb_ild.aop;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.entity.AuditEntry;
import org.mifos.creditbureau.cb_ild.repository.AuditEntryRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuditPersistenceService.
 *
 * Test 1: saveAuditEntry stores all fields correctly
 * Test 2: result SUCCESS stored correctly
 * Test 3: result FAILURE with errorMessage stored correctly
 * Test 4: null errorMessage stored as null — not blank string
 * Test 5: repository.save called exactly once per saveAuditEntry
 */
@ExtendWith(MockitoExtension.class)
class AuditPersistenceServiceTest {

    @Mock
    private AuditEntryRepository auditEntryRepository;

    private AuditPersistenceService service;

    @BeforeEach
    void setUp() {
        service = new AuditPersistenceService(auditEntryRepository);
        when(auditEntryRepository.save(any(AuditEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("saveAuditEntry stores all fields correctly")
    void saveAuditEntry_allFields_storedCorrectly() {
        service.saveAuditEntry(
                "CDC_SCORE_PULL",
                "BureauResponse",
                "user123",
                "req-abc-123",
                250L,
                "SUCCESS",
                null);

        ArgumentCaptor<AuditEntry> captor =
                ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());

        AuditEntry saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("CDC_SCORE_PULL");
        assertThat(saved.getEntityType()).isEqualTo("BureauResponse");
        assertThat(saved.getPerformedBy()).isEqualTo("user123");
        assertThat(saved.getRequestId()).isEqualTo("req-abc-123");
        assertThat(saved.getDurationMs()).isEqualTo(250L);
        assertThat(saved.getResult()).isEqualTo("SUCCESS");
        assertThat(saved.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("result SUCCESS stored correctly")
    void saveAuditEntry_resultSuccess_storedCorrectly() {
        service.saveAuditEntry(
                "CDC_SCORE_PULL", "BureauResponse",
                "user123", "req-123", 100L, "SUCCESS", null);

        ArgumentCaptor<AuditEntry> captor =
                ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getResult()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("result FAILURE with errorMessage stored correctly")
    void saveAuditEntry_resultFailure_withErrorMessage() {
        service.saveAuditEntry(
                "CDC_SCORE_PULL", "BureauResponse",
                "user123", "req-123", 150L,
                "FAILURE", "KYC prerequisite not met");

        ArgumentCaptor<AuditEntry> captor =
                ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());

        AuditEntry saved = captor.getValue();
        assertThat(saved.getResult()).isEqualTo("FAILURE");
        assertThat(saved.getErrorMessage())
                .isEqualTo("KYC prerequisite not met");
    }

    @Test
    @DisplayName("null errorMessage stored as null — not blank string")
    void saveAuditEntry_nullErrorMessage_storedAsNull() {
        service.saveAuditEntry(
                "CDC_SCORE_PULL", "BureauResponse",
                "user123", "req-123", 100L, "SUCCESS", null);

        ArgumentCaptor<AuditEntry> captor =
                ArgumentCaptor.forClass(AuditEntry.class);
        verify(auditEntryRepository).save(captor.capture());
        assertThat(captor.getValue().getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("repository.save called exactly once per saveAuditEntry")
    void saveAuditEntry_repositorySave_calledExactlyOnce() {
        service.saveAuditEntry(
                "CDC_SCORE_PULL", "BureauResponse",
                "anonymous", "req-456", 200L, "SUCCESS", null);

        verify(auditEntryRepository).save(any(AuditEntry.class));
    }
}
