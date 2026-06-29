package org.mifos.creditbureau.cb_ild.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;
import org.mifos.creditbureau.cb_ild.repository.AuditEntryRepository;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.service.bureau.IBureauReadinessService;
import org.mifos.creditbureau.cb_ild.service.kyc.KycReadinessResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for BureauReadinessController.
 *
 * Test 1: valid clientId — HTTP 200 + KycReadinessResult returned
 * Test 2: service returns score=0 ready=false — HTTP 200 (not error)
 * Test 3: FineractNotFoundException — propagated to GlobalExceptionHandler
 * Test 4: result body is exactly what service returns — no mutation
 */
@ExtendWith(MockitoExtension.class)
class BureauReadinessControllerTest {

    @Mock
    private IBureauReadinessService bureauReadinessService;
    @Mock
    private BureauResponseRepository bureauResponseRepository;
    @Mock
    private AuditEntryRepository auditEntryRepository;

    private BureauReadinessController controller;

    private static final Long CLIENT_ID = 1L;

    @BeforeEach
    void setUp() {
        controller = new BureauReadinessController(bureauReadinessService, bureauResponseRepository, auditEntryRepository);
    }

    private KycReadinessResult fullResult() {
        return new KycReadinessResult(
                CLIENT_ID, 100, true, List.of(),
                750, "LOW", false, Instant.now()
        );
    }

    @Test
    @DisplayName("valid clientId — HTTP 200 + KycReadinessResult returned")
    void getBureauReadiness_validClientId_returns200WithResult() {
        KycReadinessResult expected = fullResult();
        when(bureauReadinessService.checkReadiness(CLIENT_ID))
                .thenReturn(expected);

        ResponseEntity<KycReadinessResult> response =
                controller.getBureauReadiness(CLIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
    }

    @Test
    @DisplayName("score=0 ready=false — HTTP 200 not error")
    void getBureauReadiness_scoreZero_returns200NotError() {
        KycReadinessResult rfcMissing = new KycReadinessResult(
                CLIENT_ID, 0, false,
                List.of("nationalId"),
                null, null, false, null
        );
        when(bureauReadinessService.checkReadiness(CLIENT_ID))
                .thenReturn(rfcMissing);

        ResponseEntity<KycReadinessResult> response =
                controller.getBureauReadiness(CLIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().score()).isEqualTo(0);
        assertThat(response.getBody().ready()).isFalse();
    }

    @Test
    @DisplayName("FineractNotFoundException — propagated to GlobalExceptionHandler")
    void getBureauReadiness_fineractNotFound_exceptionPropagated() {
        when(bureauReadinessService.checkReadiness(CLIENT_ID))
                .thenThrow(new FineractNotFoundException(CLIENT_ID));

        assertThatThrownBy(() -> controller.getBureauReadiness(CLIENT_ID))
                .isInstanceOf(FineractNotFoundException.class);
    }

    @Test
    @DisplayName("result body not mutated — exactly what service returns")
    void getBureauReadiness_resultBody_notMutated() {
        KycReadinessResult expected = fullResult();
        when(bureauReadinessService.checkReadiness(CLIENT_ID))
                .thenReturn(expected);

        ResponseEntity<KycReadinessResult> response =
                controller.getBureauReadiness(CLIENT_ID);

        assertThat(response.getBody()).isSameAs(expected);
    }
}
