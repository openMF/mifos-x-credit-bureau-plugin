package org.mifos.creditbureau.cb_ild.service.dispute;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.entity.DisputeCase;
import org.mifos.creditbureau.cb_ild.entity.enums.DisputeStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;
import org.mifos.creditbureau.cb_ild.entity.SubmissionRecord;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.mifos.creditbureau.cb_ild.repository.DisputeCaseRepository;
import org.mifos.creditbureau.cb_ild.repository.SubmissionRecordRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DisputeServiceImpl (MX-275).
 *
 * Follows exact same pattern as BureauReadinessServiceTest and
 * SubmissionServiceTest: @ExtendWith(MockitoExtension.class),
 * constructor injection in setUp(), private fixtures, @DisplayName.
 *
 * Test 1:  createDispute null submissionRecordId -> IllegalArgumentException
 * Test 2:  createDispute blank disputeDetails -> IllegalArgumentException
 * Test 3:  createDispute blank raisedBy -> IllegalArgumentException
 * Test 4:  createDispute submission not found -> IllegalArgumentException
 * Test 5:  createDispute valid -> OPEN, openedAt set, raisedBy set
 * Test 6:  createDispute no bureau response -> cdcData null, still saves
 * Test 7:  updateStatus null disputeId -> IllegalArgumentException
 * Test 8:  updateStatus OPEN->UNDER_REVIEW -> succeeds
 * Test 9:  updateStatus UNDER_REVIEW->RESOLVED -> resolvedAt set
 * Test 10: updateStatus UNDER_REVIEW->OPEN -> IllegalStateException
 */
@ExtendWith(MockitoExtension.class)
class DisputeServiceImplTest {

    @Mock
    private DisputeCaseRepository disputeCaseRepository;

    @Mock
    private SubmissionRecordRepository submissionRecordRepository;

    @Mock
    private BureauResponseRepository bureauResponseRepository;

    @Mock
    private FineractApiClient fineractApiClient;

    private DisputeServiceImpl service;

    private static final Long SUBMISSION_ID = 1L;
    private static final Long CLIENT_ID = 10L;
    private static final Long DISPUTE_ID = 99L;

    @BeforeEach
    void setUp() {
        service = new DisputeServiceImpl(
                disputeCaseRepository,
                submissionRecordRepository,
                bureauResponseRepository,
                fineractApiClient,
                new ObjectMapper());
    }

    private SubmissionRecord submission() {
        return SubmissionRecord.builder()
                .id(SUBMISSION_ID)
                .clientId(CLIENT_ID)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.ACCEPTED)
                .retryCount(0)
                .build();
    }

    private FineractClientData clientData() {
        return new FineractClientData(
                CLIENT_ID, "Juan", "Perez", "ABCD123456XYZ",
                List.of(1990, 5, 15), "5551234567", null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null);
    }

    private BureauResponseEntity bureauResponse() {
        return BureauResponseEntity.builder()
                .clientId(CLIENT_ID)
                .ficoScore(750)
                .riskBand("LOW")
                .hasDelinquencies(false)
                .pulledAt(LocalDateTime.now())
                .build();
    }

    private DisputeCase openDispute() {
        return DisputeCase.builder()
                .id(DISPUTE_ID)
                .submissionRecordId(SUBMISSION_ID)
                .status(DisputeStatus.OPEN)
                .disputeDetails("Score mismatch")
                .raisedBy("user-123")
                .openedAt(LocalDateTime.now())
                .build();
    }

    private DisputeCase underReviewDispute() {
        return DisputeCase.builder()
                .id(DISPUTE_ID)
                .submissionRecordId(SUBMISSION_ID)
                .status(DisputeStatus.UNDER_REVIEW)
                .disputeDetails("Score mismatch")
                .raisedBy("user-123")
                .openedAt(LocalDateTime.now())
                .build();
    }

    // ===== createDispute tests =====

    @Test
    @DisplayName("null submissionRecordId -> IllegalArgumentException")
    void createDispute_nullSubmissionRecordId_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.createDispute(null, "details", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("submissionRecordId must not be null");
        verify(submissionRecordRepository, never()).findById(any());
    }

    @Test
    @DisplayName("blank disputeDetails -> IllegalArgumentException")
    void createDispute_blankDisputeDetails_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.createDispute(SUBMISSION_ID, "  ", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disputeDetails must not be null or blank");
        verify(submissionRecordRepository, never()).findById(any());
    }

    @Test
    @DisplayName("blank raisedBy -> IllegalArgumentException")
    void createDispute_blankRaisedBy_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.createDispute(SUBMISSION_ID, "details", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("raisedBy must not be null or blank");
        verify(submissionRecordRepository, never()).findById(any());
    }

    @Test
    @DisplayName("submission not found -> IllegalArgumentException")
    void createDispute_submissionNotFound_throwsIllegalArgument() {
        when(submissionRecordRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.createDispute(SUBMISSION_ID, "details", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SubmissionRecord not found");
        verify(disputeCaseRepository, never()).save(any());
    }

    @Test
    @DisplayName("valid args -> status=OPEN, openedAt set, raisedBy set")
    void createDispute_valid_savedAsOpen() {
        when(submissionRecordRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(submission()));
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(bureauResponseRepository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.of(bureauResponse()));
        when(disputeCaseRepository.save(any(DisputeCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DisputeCase result = service.createDispute(
                SUBMISSION_ID, "Score mismatch", "user-123");

        assertThat(result.getStatus()).isEqualTo(DisputeStatus.OPEN);
        assertThat(result.getOpenedAt()).isNotNull();
        assertThat(result.getRaisedBy()).isEqualTo("user-123");
        assertThat(result.getSubmissionRecordId()).isEqualTo(SUBMISSION_ID);
        assertThat(result.getExpiryDate()).isNotNull();
    }

    @Test
    @DisplayName("no bureau response -> cdcData null, dispute still saves")
    void createDispute_noBureauResponse_cdcDataNullStillSaves() {
        when(submissionRecordRepository.findById(SUBMISSION_ID))
                .thenReturn(Optional.of(submission()));
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(bureauResponseRepository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.empty());
        when(disputeCaseRepository.save(any(DisputeCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DisputeCase result = service.createDispute(
                SUBMISSION_ID, "Score mismatch", "user-123");

        assertThat(result.getStatus()).isEqualTo(DisputeStatus.OPEN);
        assertThat(result.getCdcData()).isNull();
        verify(disputeCaseRepository).save(any());
    }

    // ===== updateStatus tests =====

    @Test
    @DisplayName("null disputeId -> IllegalArgumentException")
    void updateStatus_nullDisputeId_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                service.updateStatus(null, "UNDER_REVIEW", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disputeId must not be null");
        verify(disputeCaseRepository, never()).findById(any());
    }

    @Test
    @DisplayName("OPEN -> UNDER_REVIEW -> succeeds, status updated")
    void updateStatus_openToUnderReview_succeeds() {
        when(disputeCaseRepository.findById(DISPUTE_ID))
                .thenReturn(Optional.of(openDispute()));
        when(disputeCaseRepository.save(any(DisputeCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DisputeCase result = service.updateStatus(
                DISPUTE_ID, "UNDER_REVIEW", null);

        assertThat(result.getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
        assertThat(result.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("UNDER_REVIEW -> RESOLVED -> resolvedAt set, notes set")
    void updateStatus_underReviewToResolved_resolvedAtSet() {
        when(disputeCaseRepository.findById(DISPUTE_ID))
                .thenReturn(Optional.of(underReviewDispute()));
        when(disputeCaseRepository.save(any(DisputeCase.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        DisputeCase result = service.updateStatus(
                DISPUTE_ID, "RESOLVED", "Verified with CDC");

        assertThat(result.getStatus()).isEqualTo(DisputeStatus.RESOLVED);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(result.getResolutionNotes()).isEqualTo("Verified with CDC");
    }

    @Test
    @DisplayName("UNDER_REVIEW -> OPEN -> IllegalStateException (no backwards)")
    void updateStatus_underReviewToOpen_throwsIllegalState() {
        when(disputeCaseRepository.findById(DISPUTE_ID))
                .thenReturn(Optional.of(underReviewDispute()));

        assertThatThrownBy(() ->
                service.updateStatus(DISPUTE_ID, "OPEN", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNDER_REVIEW -> OPEN");
        verify(disputeCaseRepository, never()).save(any());
    }
}
