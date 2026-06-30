package org.mifos.creditbureau.cb_ild.service.bureau;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.client.FineractApiClient;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.service.cdc.ICdcScorePullService;
import org.mifos.creditbureau.cb_ild.service.kyc.IKycScoringService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BureauReadinessService.
 *
 * Test 1: null clientId — IllegalArgumentException
 * Test 2: score < 70 — CDC never called, kycResult returned directly
 * Test 3: score >= 70 — CDC called, entity fields mapped to result
 * Test 4: scoreDropAlert true — mapped correctly
 * Test 5: pulledAt LocalDateTime — converted to Instant UTC
 * Test 6: pulledAt null — pulledAt in result is null
 * Test 7: FineractNotFoundException — propagated to caller
 * Test 8: RFC missing (score=0) — CDC never called
 */
@ExtendWith(MockitoExtension.class)
class BureauReadinessServiceTest {

    @Mock
    private FineractApiClient fineractApiClient;

    @Mock
    private IKycScoringService kycScoringService;

    @Mock
    private ICdcScorePullService cdcScorePullService;

    private BureauReadinessService service;

    private static final Long CLIENT_ID = 1L;

    @BeforeEach
    void setUp() {
        service = new BureauReadinessService(
                fineractApiClient, kycScoringService, cdcScorePullService);
    }

    private FineractClientData clientData() {
        return new FineractClientData(
                CLIENT_ID, "Juan", "Perez", "ABCD123456XYZ",
                List.of(1990, 5, 15), "5551234567", null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );
    }

    private KycReadinessResult kycResult(int score, boolean ready) {
        return new KycReadinessResult(
                CLIENT_ID, score, ready, List.of(),
                null, null, false, null
        );
    }

    private BureauResponseEntity bureauEntity(
            Integer ficoScore, String riskBand,
            Boolean scoreDropAlert, LocalDateTime pulledAt) {
        return BureauResponseEntity.builder()
                .clientId(CLIENT_ID)
                .ficoScore(ficoScore)
                .riskBand(riskBand)
                .scoreDropAlert(scoreDropAlert)
                .pulledAt(pulledAt)
                .build();
    }

    @Test
    @DisplayName("null clientId — IllegalArgumentException")
    void checkReadiness_nullClientId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.checkReadiness(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientId must not be null");
    }

    @Test
    @DisplayName("score < 70 — CDC never called, kycResult returned directly")
    void checkReadiness_scoreBelowThreshold_cdcNeverCalled() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycResult(50, false));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.ready()).isFalse();
        assertThat(result.ficoScore()).isNull();
        verify(cdcScorePullService, never()).pullAndSave(any());
    }

    @Test
    @DisplayName("score >= 70 — CDC called, entity fields mapped to result")
    void checkReadiness_scoreAboveThreshold_cdcCalledAndMapped() {
        LocalDateTime pulledAt = LocalDateTime.of(2026, 6, 8, 10, 30, 0);

        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycResult(100, true));
        when(cdcScorePullService.pullAndSave(CLIENT_ID))
                .thenReturn(bureauEntity(750, "LOW", false, pulledAt));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.ready()).isTrue();
        assertThat(result.ficoScore()).isEqualTo(750);
        assertThat(result.riskBand()).isEqualTo("LOW");
        assertThat(result.scoreDropAlert()).isFalse();
        assertThat(result.pulledAt()).isNotNull();
        verify(cdcScorePullService).pullAndSave(CLIENT_ID);
    }

    @Test
    @DisplayName("scoreDropAlert true — mapped correctly to result")
    void checkReadiness_scoreDropAlert_mappedCorrectly() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycResult(100, true));
        when(cdcScorePullService.pullAndSave(CLIENT_ID))
                .thenReturn(bureauEntity(700, "MEDIUM", true,
                        LocalDateTime.now()));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.scoreDropAlert()).isTrue();
    }

    @Test
    @DisplayName("pulledAt LocalDateTime — converted to Instant UTC correctly")
    void checkReadiness_pulledAt_convertedToInstantUtc() {
        LocalDateTime pulledAt = LocalDateTime.of(2026, 6, 8, 10, 30, 0);
        Instant expected = pulledAt.atZone(ZoneOffset.UTC).toInstant();

        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycResult(100, true));
        when(cdcScorePullService.pullAndSave(CLIENT_ID))
                .thenReturn(bureauEntity(750, "LOW", false, pulledAt));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.pulledAt()).isEqualTo(expected);
    }

    @Test
    @DisplayName("pulledAt null in entity — pulledAt null in result")
    void checkReadiness_pulledAtNull_resultPulledAtNull() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(kycResult(100, true));
        when(cdcScorePullService.pullAndSave(CLIENT_ID))
                .thenReturn(bureauEntity(750, "LOW", false, null));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.pulledAt()).isNull();
    }

    @Test
    @DisplayName("FineractNotFoundException — propagated to caller")
    void checkReadiness_fineractNotFound_propagated() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenThrow(new FineractNotFoundException(CLIENT_ID));

        assertThatThrownBy(() -> service.checkReadiness(CLIENT_ID))
                .isInstanceOf(FineractNotFoundException.class);

        verify(cdcScorePullService, never()).pullAndSave(any());
    }

    @Test
    @DisplayName("RFC missing (score=0) — CDC never called")
    void checkReadiness_rfcMissing_cdcNeverCalled() {
        when(fineractApiClient.getClientData(CLIENT_ID))
                .thenReturn(clientData());
        when(kycScoringService.score(any(FineractClientData.class)))
                .thenReturn(new KycReadinessResult(
                        CLIENT_ID, 0, false,
                        List.of("nationalId"),
                        null, null, false, null
                ));

        KycReadinessResult result = service.checkReadiness(CLIENT_ID);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.ready()).isFalse();
        assertThat(result.missingFields()).contains("nationalId");
        verify(cdcScorePullService, never()).pullAndSave(any());
    }
}
