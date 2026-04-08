package org.mifos.creditbureau.service.connectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link ConnectorRegistry}.
 * <p>
 * Uses H2 + Liquibase (via {@code @DataJpaTest}) to verify that the
 * registry correctly resolves connectors using real database lookups.
 * A lightweight {@link StubConnector} is used instead of the full
 * {@code CirculoDeCreditoConnector} to keep the test focused on
 * registry wiring, not connector internals.
 */
@DataJpaTest
@Import(ConnectorRegistryIntegrationTest.TestConfig.class)
class ConnectorRegistryIntegrationTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        public CreditBureauConnector stubCDCConnector() {
            return new StubConnector("CIRCULO_DE_CREDITO");
        }

        @Bean
        public ConnectorRegistry connectorRegistry(
                List<CreditBureauConnector> connectors,
                CreditBureauRepository repo) {
            return new ConnectorRegistry(connectors, repo);
        }
    }

    @Autowired
    private ConnectorRegistry registry;

    @Autowired
    private CreditBureauRepository creditBureauRepository;

    @Test
    @DisplayName("Spring context loads and registry contains CDC connector")
    void contextLoads_registryContainsCDCConnector() {
        assertThat(registry).isNotNull();
        assertThat(registry.getSupportedTypes()).contains("CIRCULO_DE_CREDITO");
    }

    @Test
    @DisplayName("getConnector: saved bureau with CDC type resolves correctly")
    void getConnector_savedBureauWithCDCType_resolves() {
        CreditBureau bureau = new CreditBureau();
        bureau.setCreditBureauName("CDC Test");
        bureau.setActive(true);
        bureau.setCountry("Mexico");
        bureau.setBureauType("CIRCULO_DE_CREDITO");
        CreditBureau saved = creditBureauRepository.save(bureau);

        CreditBureauConnector connector = registry.getConnector(saved.getId());

        assertThat(connector).isNotNull();
        assertThat(connector.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
    }

    @Test
    @DisplayName("getConnector: unknown ID throws IllegalArgumentException")
    void getConnector_unknownId_throwsException() {
        assertThatThrownBy(() -> registry.getConnector(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit Bureau not found");
    }

    @Test
    @DisplayName("getSupportedTypes: contains CIRCULO_DE_CREDITO")
    void supportedTypes_containsCIRCULO_DE_CREDITO() {
        assertThat(registry.getSupportedTypes()).containsExactly("CIRCULO_DE_CREDITO");
    }

    @Test
    @DisplayName("Registry resolves correctly with real H2 database lookup")
    void registry_resolves_with_realDatabaseLookup() {
        // Save two bureaus with different types
        CreditBureau cdc = new CreditBureau();
        cdc.setCreditBureauName("Circulo de Credito");
        cdc.setActive(true);
        cdc.setCountry("Mexico");
        cdc.setBureauType("CIRCULO_DE_CREDITO");
        CreditBureau savedCdc = creditBureauRepository.save(cdc);

        // Unsupported type — registry should reject
        CreditBureau unknown = new CreditBureau();
        unknown.setCreditBureauName("Unknown Bureau");
        unknown.setActive(true);
        unknown.setCountry("US");
        unknown.setBureauType("EQUIFAX");
        CreditBureau savedUnknown = creditBureauRepository.save(unknown);

        // CDC resolves
        assertThat(registry.getConnector(savedCdc.getId()).getBureauType())
                .isEqualTo("CIRCULO_DE_CREDITO");

        // EQUIFAX has no registered connector → exception
        assertThatThrownBy(() -> registry.getConnector(savedUnknown.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No connector registered for bureau type: EQUIFAX");
    }

    /**
     * Lightweight stub connector for integration testing.
     * Tests registry resolution, not connector behavior.
     */
    private static class StubConnector implements CreditBureauConnector {
        private final String bureauType;

        StubConnector(String bureauType) {
            this.bureauType = bureauType;
        }

        @Override
        public String getBureauType() {
            return bureauType;
        }

        @Override
        public CreditReportResult fetchCreditReport(Long creditBureauId, ClientData clientData) {
            return CreditReportResult.builder().success(true).bureauType(bureauType).build();
        }

        @Override
        public ConnectionTestResult testConnection(Long creditBureauId) {
            return ConnectionTestResult.builder().success(true).bureauType(bureauType).build();
        }
    }
}
