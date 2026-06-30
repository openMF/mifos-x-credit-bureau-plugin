package org.mifos.creditbureau.cb_ild.service.submission;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;
import org.mifos.creditbureau.cb_ild.service.kyc.IKycScoringService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SubmissionServiceImpl.
 *
 * Follows the exact same pattern as BureauReadinessServiceTest:
 *   @ExtendWith(MockitoExtension.class), constructor injection in setUp(),
 *   private fixture methods, @DisplayName on every test.
 *
 * mockEnabled=true for all tests (default, same as application.properties).
 * Real-mode (CDC call) is not tested here — it throws CdcNotConfiguredException
 * by design until Victor's credentials are registered (Phase 2/MX-276).
 *
 * Test 1:  null clientId — IllegalArgumentException
 * Test 2:  null triggerType — IllegalArgumentException
 * Test 3:  SCREENING_EVENT + null inquiryType — IllegalArgumentException
 * Test 4:  KYC score < 70 — REJECTED, CDC never called, rejectionReason set
 * Test 5:  KYC score 0 (RFC missing) — REJECTED, CDC never called
 * Test 6:  KYC score >= 70, mock — ACCEPTED, cdcReferenceId starts with MOCK-
 * Test 7:  KYC score >= 70, mock — updatedAt and submittedAt not null
 * Test 8:  FineractNotFoundException — propagated, repository never called
 * Test 9:  retrySubmission null — IllegalArgumentException
 * Test 10: retrySubmission wrong status — IllegalArgumentException
 * Test 11: retrySubmission PENDING_RETRY, mock — ACCEPTED, cdcReferenceId set
 * Test 12: retrySubmission PENDING_RETRY, mock success — retryCount unchanged
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock
    private SubmissionRecordRepository submissionRecordRepository;

    @Mock
    private FineractApiClient fineractApiClient;

    @Mock
    private IKycScoringService kycScoringService;

    @Mock
    private SubmissionRetryProperties retryProperties;
    @Mock
    private org.mifos.creditbureau.cb_ild.client.CdcPluginClient cdcPluginClient;

    private SubmissionServiceImpl service;

    private static final Long CLIENT_ID = 1L;

    @BeforeEach
    void setUp() {
        when(retryProperties.getMaxAttempts()).thenReturn(3);
        when(retryProperties.getRetryIntervalMinutes()).thenReturn(60);

        service = new SubmissionServiceImpl(
                submissionRecordRepository,
                fineractApiClient,
                kycScoringService,
                retryProperties,
                true,
                cdcPluginClient
        );
    }

    private FineractClientData clientData() {
        return new FineractClientData(
                CLIENT_ID, "Juan", "Perez", "ABCD123456XYZ",
                List.of(1990, 5, 15), "5551234567", null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );
    }

    private FineractClientData clientDataNoRfc() {
        return new FineractClientData(
                CLIENT_ID, "Maria", "Lopez", null,
                null, null, null,
                null, null, null,
                null, null, null, null
        );
    }

    private KycReadinessResult kycReady() {
        return new KycReadinessResult(
                CLIENT_ID, 100, true, List.of(),
                null, null, false, null
        );
    }

    private KycReadinessResult kycNotReady(int score, List<String> missing) {
        return new KycReadinessResult(
                CLIENT_ID, score, false, missing,
                null, null, false, null
        );
    }

    private SubmissionRecord pendingRetryRecord() {
        return SubmissionRecord.builder()
                .id(1L)
                .clientId(CLIENT_ID)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.PENDING_RETRY)
                .retryCount(1)
                .updatedAt(LocalDateTime.now().minusHours(2))
                .build();
    }

    @Test
    @DisplayName("null clientId — IllegalArgumentException")
    void submitClient_nullClientId_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.submitClient(null, TriggerType.MANUAL_BATCH, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId must not be null");
        verify(fineractApiClient, never()).getClientData(any());
    }

    @Test
    @DisplayName("null triggerType — IllegalArgumentException")
    void submitClient_nullTriggerType_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.submitClient(CLIENT_ID, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("triggerType must not be null");
        verify(fineractApiClient, never()).getClientData(any());
    }

    @Test
    @DisplayName("SCREENING_EVENT + null inquiryType — IllegalArgumentException")
    void submitClient_screeningEventNullInquiry_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.submitClient(CLIENT_ID, TriggerType.SCREENING_EVENT, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inquiryType is required");
        verify(fineractApiClient, never()).getClientData(any());
    }

    @Test
    @DisplayName("KYC score < 70 — REJECTED, CDC never called, rejectionReason set")
    void submitClient_scoreBelowThreshold_savedAsRejected() {
        when(fineractApiClient.getClientData(CLIENT_ID)).thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycNotReady(50, List.of("address")));
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.submitClient(
                CLIENT_ID, TriggerType.MANUAL_BATCH, null, null);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(result.getRejectionReason()).contains("50");
        assertThat(result.getRejectionReason()).contains("address");
        assertThat(result.getCdcReferenceId()).isNull();
        assertThat(result.getRetryCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("KYC score 0 (RFC missing) — REJECTED, CDC never called")
    void submitClient_rfcMissing_savedAsRejected() {
        when(fineractApiClient.getClientData(CLIENT_ID)).thenReturn(clientDataNoRfc());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycNotReady(0, List.of("nationalId")));
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.submitClient(
                CLIENT_ID, TriggerType.MANUAL_BATCH, null, null);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.REJECTED);
        assertThat(result.getRejectionReason()).contains("nationalId");
        assertThat(result.getCdcReferenceId()).isNull();
    }

    @Test
    @DisplayName("KYC score >= 70, mock — ACCEPTED, cdcReferenceId starts with MOCK-")
    void submitClient_scoreAboveThreshold_savedAsAccepted() {
        when(fineractApiClient.getClientData(CLIENT_ID)).thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycReady());
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.submitClient(
                CLIENT_ID, TriggerType.MANUAL_BATCH, null, null);

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(result.getCdcReferenceId()).startsWith("MOCK-");
        assertThat(result.getRejectionReason()).isNull();
    }

    @Test
    @DisplayName("KYC score >= 70, mock — updatedAt and submittedAt not null")
    void submitClient_scoreAboveThreshold_timestampsSet() {
        when(fineractApiClient.getClientData(CLIENT_ID)).thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycReady());
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.submitClient(
                CLIENT_ID, TriggerType.LOAN_APPROVAL, 42L, null);

        assertThat(result.getUpdatedAt()).isNotNull();
        assertThat(result.getSubmittedAt()).isNotNull();
        assertThat(result.getLoanId()).isEqualTo(42L);
        assertThat(result.getTriggerType()).isEqualTo(TriggerType.LOAN_APPROVAL);
    }

    @Test
    @DisplayName("FineractNotFoundException — propagated, repository never called")
    void submitClient_fineractNotFound_propagated() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenThrow(new FineractNotFoundException(CLIENT_ID));

        assertThatThrownBy(() ->
                service.submitClient(CLIENT_ID, TriggerType.MANUAL_BATCH, null, null))
                .isInstanceOf(FineractNotFoundException.class);

        verify(submissionRecordRepository, never()).save(any());
    }

    @Test
    @DisplayName("retrySubmission null — IllegalArgumentException")
    void retrySubmission_null_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.retrySubmission(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("existing must not be null");
    }

    @Test
    @DisplayName("retrySubmission wrong status — IllegalArgumentException")
    void retrySubmission_wrongStatus_throwsIllegalArgument() {
        SubmissionRecord accepted = SubmissionRecord.builder()
                .id(1L)
                .clientId(CLIENT_ID)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.ACCEPTED)
                .retryCount(0)
                .build();

        assertThatThrownBy(() -> service.retrySubmission(accepted))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PENDING_RETRY");
    }

    @Test
    @DisplayName("retrySubmission PENDING_RETRY, mock — ACCEPTED, cdcReferenceId set")
    void retrySubmission_pendingRetry_savedAsAccepted() {
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.retrySubmission(pendingRetryRecord());

        assertThat(result.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(result.getCdcReferenceId()).startsWith("MOCK-");
        assertThat(result.getRejectionReason()).isNull();
        assertThat(result.getSubmittedAt()).isNotNull();
    }

    @Test
    @DisplayName("retrySubmission PENDING_RETRY, mock success — retryCount unchanged")
    void retrySubmission_mockSuccess_retryCountUnchanged() {
        when(submissionRecordRepository.save(any(SubmissionRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        SubmissionRecord result = service.retrySubmission(pendingRetryRecord());

        assertThat(result.getRetryCount()).isEqualTo(1);
        assertThat(result.getUpdatedAt()).isNotNull();
    }
}
