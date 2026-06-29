package org.mifos.creditbureau.cb_ild.service.cdc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.entity.BureauResponseEntity;
import org.mifos.creditbureau.cb_ild.repository.BureauResponseRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CdcScorePullServiceImpl.
 *
 * Test 1: Mock mode — entity saved with ficoScore=750
 * Test 2: Mock mode — rawResponseHash not null, exactly 64 chars
 * Test 3: Previous score 800, new 750 — scoreDropAlert=true
 * Test 4: No previous response — scoreDropAlert=false
 * Test 5: clientId null — IllegalArgumentException
 *
 * Pure unit tests — no Spring context, no real DB.
 * Repository mocked with Mockito.
 * Service constructed manually in @BeforeEach.
 */
@ExtendWith(MockitoExtension.class)
class CdcScorePullServiceTest {

    @Mock
    private BureauResponseRepository repository;
    @Mock
    private org.mifos.creditbureau.cb_ild.client.CdcPluginClient cdcPluginClient;

    private CdcScorePullServiceImpl service;

    private static final Long CLIENT_ID = 1L;
    private static final boolean MOCK_ENABLED = true;

    @BeforeEach
    void setUp() {
        // Construct manually — @Value not available in unit tests
        service = new CdcScorePullServiceImpl(repository, MOCK_ENABLED, cdcPluginClient);
    }

    // ===== TEST 1 — Mock mode saves entity =====

    @Test
    @DisplayName("Mock mode — entity saved with ficoScore=750 and bureauType correct")
    void pullAndSave_mockMode_savesEntityWithCorrectFicoScore() {
        // No previous response — first pull
        when(repository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.empty());

        // repository.save returns the entity passed to it
        when(repository.save(any(BureauResponseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BureauResponseEntity result = service.pullAndSave(CLIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.getClientId()).isEqualTo(CLIENT_ID);
        assertThat(result.getFicoScore()).isEqualTo(750);
        assertThat(result.getRiskBand()).isEqualTo("LOW");
        assertThat(result.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
        assertThat(result.getSoftDeleted()).isFalse();
        assertThat(result.getHasDelinquencies()).isFalse();
    }

    // ===== TEST 2 — rawResponseHash is SHA-256 =====

    @Test
    @DisplayName("Mock mode — rawResponseHash not null and exactly 64 characters")
    void pullAndSave_mockMode_rawResponseHashIs64CharSha256() {
        when(repository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.empty());
        when(repository.save(any(BureauResponseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BureauResponseEntity result = service.pullAndSave(CLIENT_ID);

        // SHA-256 always produces exactly 64 hex characters
        assertThat(result.getRawResponseHash()).isNotNull();
        assertThat(result.getRawResponseHash()).hasSize(64);
        // Must not be blank or mock placeholder
        assertThat(result.getRawResponseHash()).isNotBlank();
    }

    // ===== TEST 3 — Score drop detection =====

    @Test
    @DisplayName("Previous score 800, new score 750 — scoreDropAlert=true")
    void pullAndSave_previousScore800_newScore750_scoreDropAlertTrue() {
        // Previous response with ficoScore=800
        BureauResponseEntity previous = BureauResponseEntity.builder()
                .clientId(CLIENT_ID)
                .ficoScore(800)
                .bureauType("CIRCULO_DE_CREDITO")
                .softDeleted(false)
                .hasDelinquencies(false)
                .scoreDropAlert(false)
                .build();

        when(repository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.of(previous));
        when(repository.save(any(BureauResponseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // New mock score = 750 < 800 → scoreDropAlert must be true
        BureauResponseEntity result = service.pullAndSave(CLIENT_ID);

        assertThat(result.getScoreDropAlert()).isTrue();
    }

    // ===== TEST 4 — No previous response =====

    @Test
    @DisplayName("No previous response — scoreDropAlert=false")
    void pullAndSave_noPreviousResponse_scoreDropAlertFalse() {
        // First time this client has been checked
        when(repository.findTopByClientIdOrderByPulledAtDesc(CLIENT_ID))
                .thenReturn(Optional.empty());
        when(repository.save(any(BureauResponseEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BureauResponseEntity result = service.pullAndSave(CLIENT_ID);

        assertThat(result.getScoreDropAlert()).isFalse();
    }

    // ===== TEST 5 — Null clientId =====

    @Test
    @DisplayName("clientId null — IllegalArgumentException thrown immediately")
    void pullAndSave_nullClientId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> service.pullAndSave(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clientId must not be null");
    }
}
