package org.mifos.creditbureau.cb_ild.service.kyc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.cb_ild.client.FineractClientData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for KycCompletenessScorer.
 *
 * Test 1:  null clientData — IllegalArgumentException
 * Test 2:  RFC missing — score=0, ready=false, missingFields=[nationalId]
 * Test 3:  blank RFC — score=0, ready=false, missingFields=[nationalId]
 * Test 4:  RFC only — score=30, ready=false, 5 missing fields
 * Test 5:  all fields — score=100, ready=true, no missing fields
 * Test 6:  RFC + DOB + names + address — score=95, ready=true
 * Test 7:  RFC + DOB only — score=50, ready=false
 * Test 8:  score exactly 70 — ready=true (boundary)
 * Test 9:  score exactly 65 — ready=false (below threshold)
 * Test 10: missingFields never contains actual RFC value — PII
 * Test 11: missingFields is immutable
 * Test 12: ficoScore/riskBand/pulledAt null — set by BureauReadinessService
 */
class KycCompletenessScorerTest {

    private KycCompletenessScorer scorer;

    @BeforeEach
    void setUp() {
        // Default weights — same as application.properties
        scorer = new KycCompletenessScorer(new KycScoringProperties());
    }

    private FineractClientData allFields() {
        return new FineractClientData(
                1L, "Juan", "Perez", "ABCD123456XYZ",
                List.of(1990, 5, 15), "5551234567", "juan@example.com",
                "Calle Reforma 123", null, null,
                "Ciudad de Mexico", "CDMX", "06600", "Mexico"
        );
    }

    private FineractClientData rfcOnly() {
        return new FineractClientData(
                1L, null, null, "ABCD123456XYZ",
                null, null, null,
                null, null, null,
                null, null, null, null
        );
    }

    @Test
    @DisplayName("null clientData — IllegalArgumentException")
    void score_nullClientData_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> scorer.score(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("clientData must not be null");
    }

    @Test
    @DisplayName("RFC missing — score=0, ready=false, missingFields=[nationalId]")
    void score_rfcMissing_score0ReadyFalse() {
        FineractClientData noRfc = new FineractClientData(
                1L, "Juan", "Perez", null,
                List.of(1990, 5, 15), "5551234567", null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );

        KycReadinessResult result = scorer.score(noRfc);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.ready()).isFalse();
        assertThat(result.missingFields()).containsExactly("nationalId");
    }

    @Test
    @DisplayName("blank RFC — score=0, ready=false, missingFields=[nationalId]")
    void score_blankRfc_score0ReadyFalse() {
        FineractClientData blankRfc = new FineractClientData(
                1L, "Juan", "Perez", "   ",
                List.of(1990, 5, 15), null, null,
                null, null, null,
                null, null, null, null
        );

        KycReadinessResult result = scorer.score(blankRfc);

        assertThat(result.score()).isEqualTo(0);
        assertThat(result.ready()).isFalse();
        assertThat(result.missingFields()).containsExactly("nationalId");
    }

    @Test
    @DisplayName("RFC only — score=30, ready=false, 5 missing fields")
    void score_rfcOnly_score30ReadyFalse() {
        KycReadinessResult result = scorer.score(rfcOnly());

        assertThat(result.score()).isEqualTo(30);
        assertThat(result.ready()).isFalse();
        assertThat(result.missingFields()).hasSize(5);
        assertThat(result.missingFields())
                .contains("dateOfBirth", "firstName", "lastName",
                        "address", "phoneNumber");
    }

    @Test
    @DisplayName("all fields present — score=100, ready=true, no missing fields")
    void score_allFields_score100ReadyTrue() {
        KycReadinessResult result = scorer.score(allFields());

        assertThat(result.score()).isEqualTo(100);
        assertThat(result.ready()).isTrue();
        assertThat(result.missingFields()).isEmpty();
    }

    @Test
    @DisplayName("RFC + DOB + names + address — score=95, ready=true")
    void score_rfcDobNamesAddress_score95ReadyTrue() {
        FineractClientData data = new FineractClientData(
                1L, "Juan", "Perez", "ABCD123456XYZ",
                List.of(1990, 5, 15), null, null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );

        KycReadinessResult result = scorer.score(data);

        assertThat(result.score()).isEqualTo(95);
        assertThat(result.ready()).isTrue();
        assertThat(result.missingFields()).containsExactly("phoneNumber");
    }

    @Test
    @DisplayName("RFC + DOB only — score=50, ready=false")
    void score_rfcDobOnly_score50ReadyFalse() {
        FineractClientData data = new FineractClientData(
                1L, null, null, "ABCD123456XYZ",
                List.of(1990, 5, 15), null, null,
                null, null, null,
                null, null, null, null
        );

        KycReadinessResult result = scorer.score(data);

        assertThat(result.score()).isEqualTo(50);
        assertThat(result.ready()).isFalse();
    }

    @Test
    @DisplayName("score exactly 70 — ready=true (boundary)")
    void score_exactly70_readyTrue() {
        // RFC(30) + DOB(20) + address(15) + phone(5) = 70
        FineractClientData data = new FineractClientData(
                1L, null, null, "ABCD123456XYZ",
                List.of(1990, 5, 15), "5551234567", null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );

        KycReadinessResult result = scorer.score(data);

        assertThat(result.score()).isEqualTo(70);
        assertThat(result.ready()).isTrue();
    }

    @Test
    @DisplayName("score exactly 65 — ready=false (below threshold)")
    void score_exactly65_readyFalse() {
        // RFC(30) + DOB(20) + address(15) = 65
        FineractClientData data = new FineractClientData(
                1L, null, null, "ABCD123456XYZ",
                List.of(1990, 5, 15), null, null,
                "Calle Reforma 123", null, null,
                "CDMX", null, null, null
        );

        KycReadinessResult result = scorer.score(data);

        assertThat(result.score()).isEqualTo(65);
        assertThat(result.ready()).isFalse();
    }

    @Test
    @DisplayName("missingFields never contains actual RFC value — PII protected")
    void score_missingFields_neverContainsRfcValue() {
        KycReadinessResult result = scorer.score(rfcOnly());

        assertThat(result.missingFields())
                .doesNotContain("ABCD123456XYZ");
    }

    @Test
    @DisplayName("missingFields is immutable — UnsupportedOperationException")
    void score_missingFields_isImmutable() {
        KycReadinessResult result = scorer.score(rfcOnly());

        assertThatThrownBy(() -> result.missingFields().add("test"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("ficoScore riskBand pulledAt null — set by BureauReadinessService")
    void score_cdcFields_areNull() {
        KycReadinessResult result = scorer.score(allFields());

        assertThat(result.ficoScore()).isNull();
        assertThat(result.riskBand()).isNull();
        assertThat(result.pulledAt()).isNull();
        assertThat(result.scoreDropAlert()).isFalse();
    }
}
