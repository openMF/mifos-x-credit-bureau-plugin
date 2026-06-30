package org.mifos.creditbureau.cb_ild.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for BureauResponseEntity.
 *
 * Test 1: Builder defaults — softDeleted, hasDelinquencies, scoreDropAlert
 * Test 2: Builder with all fields — all values set correctly
 * Test 3: ficoScore null — valid state
 * Test 4: softDeleted default false — never hard delete
 * Test 5: riskBand values — LOW/MEDIUM/HIGH/VERY_HIGH
 */
class BureauResponseEntityTest {

    @Test
    @DisplayName("Builder defaults — softDeleted false, hasDelinquencies false, scoreDropAlert false")
    void builder_defaults_allBooleanFieldsFalse() {
        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(1L)
                .bureauType("CIRCULO_DE_CREDITO")
                .build();

        assertThat(entity.getSoftDeleted()).isFalse();
        assertThat(entity.getHasDelinquencies()).isFalse();
        assertThat(entity.getScoreDropAlert()).isFalse();
    }

    @Test
    @DisplayName("Builder with all fields — all values stored correctly")
    void builder_allFields_storedCorrectly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate delinquencyDate = LocalDate.of(2020, 1, 1);

        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(1L)
                .bureauType("CIRCULO_DE_CREDITO")
                .ficoScore(750)
                .riskBand("LOW")
                .scoreDropAlert(false)
                .hasDelinquencies(true)
                .dateOfFirstDelinquency(delinquencyDate)
                .rawResponseHash("a".repeat(64))
                .softDeleted(false)
                .expiryDate(delinquencyDate.plusMonths(72))
                .build();

        assertThat(entity.getClientId()).isEqualTo(1L);
        assertThat(entity.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
        assertThat(entity.getFicoScore()).isEqualTo(750);
        assertThat(entity.getRiskBand()).isEqualTo("LOW");
        assertThat(entity.getHasDelinquencies()).isTrue();
        assertThat(entity.getDateOfFirstDelinquency()).isEqualTo(delinquencyDate);
        assertThat(entity.getRawResponseHash()).hasSize(64);
    }

    @Test
    @DisplayName("ficoScore null — valid state before CDC responds")
    void builder_ficoScoreNull_validState() {
        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(1L)
                .bureauType("CIRCULO_DE_CREDITO")
                .ficoScore(null)
                .build();

        assertThat(entity.getFicoScore()).isNull();
        assertThat(entity.getClientId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("softDeleted default false — never hard delete")
    void builder_softDeletedDefaultFalse_neverHardDelete() {
        BureauResponseEntity entity = BureauResponseEntity.builder()
                .clientId(1L)
                .bureauType("CIRCULO_DE_CREDITO")
                .build();

        assertThat(entity.getSoftDeleted()).isFalse();
        entity.setSoftDeleted(true);
        assertThat(entity.getSoftDeleted()).isTrue();
    }

    @Test
    @DisplayName("riskBand stores LOW MEDIUM HIGH VERY_HIGH correctly")
    void builder_riskBandValues_storedCorrectly() {
        for (String band : new String[]{"LOW", "MEDIUM", "HIGH", "VERY_HIGH"}) {
            BureauResponseEntity entity = BureauResponseEntity.builder()
                    .clientId(1L)
                    .bureauType("CIRCULO_DE_CREDITO")
                    .riskBand(band)
                    .build();

            assertThat(entity.getRiskBand()).isEqualTo(band);
        }
    }
}
