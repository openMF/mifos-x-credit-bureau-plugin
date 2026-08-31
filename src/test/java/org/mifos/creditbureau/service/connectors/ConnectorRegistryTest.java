package org.mifos.creditbureau.service.connectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;
import org.mifos.creditbureau.domain.CreditBureau;
import org.mifos.creditbureau.domain.CreditBureauRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ConnectorRegistry}.
 * Pure Mockito — no Spring context required.
 */
@ExtendWith(MockitoExtension.class)
class ConnectorRegistryTest {

    @Mock
    private CreditBureauRepository creditBureauRepository;

    private ConnectorRegistry registry;
    private CreditBureauConnector cdcConnector;
    private CreditBureauConnector equifaxConnector;

    @BeforeEach
    void setUp() {
        cdcConnector = new StubConnector("CIRCULO_DE_CREDITO");
        equifaxConnector = new StubConnector("EQUIFAX");
    }

    @Test
    @DisplayName("getConnector: valid bureau ID resolves to correct connector")
    void getConnector_withValidBureauId_returnsCorrectConnector() {
        registry = new ConnectorRegistry(
                List.of(cdcConnector, equifaxConnector), creditBureauRepository);

        CreditBureau bureau = new CreditBureau();
        bureau.setBureauType("CIRCULO_DE_CREDITO");
        when(creditBureauRepository.findById(1L)).thenReturn(Optional.of(bureau));

        CreditBureauConnector result = registry.getConnector(1L);

        assertThat(result).isSameAs(cdcConnector);
        assertThat(result.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
    }

    @Test
    @DisplayName("getConnector: unknown bureau ID throws IllegalArgumentException")
    void getConnector_withUnknownBureauId_throwsException() {
        registry = new ConnectorRegistry(
                List.of(cdcConnector), creditBureauRepository);

        when(creditBureauRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registry.getConnector(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Credit Bureau not found with id: 999");
    }

    @Test
    @DisplayName("getConnector: bureau exists but type has no registered connector")
    void getConnector_withUnsupportedType_throwsException() {
        registry = new ConnectorRegistry(
                List.of(cdcConnector), creditBureauRepository);

        CreditBureau bureau = new CreditBureau();
        bureau.setBureauType("UNKNOWN_BUREAU");
        when(creditBureauRepository.findById(1L)).thenReturn(Optional.of(bureau));

        assertThatThrownBy(() -> registry.getConnector(1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No connector registered for bureau type: UNKNOWN_BUREAU");
    }

    @Test
    @DisplayName("getSupportedTypes: returns all registered bureau types")
    void getSupportedTypes_returnsAllRegistered() {
        registry = new ConnectorRegistry(
                List.of(cdcConnector, equifaxConnector), creditBureauRepository);

        Set<String> types = registry.getSupportedTypes();

        assertThat(types).containsExactlyInAnyOrder("CIRCULO_DE_CREDITO", "EQUIFAX");
    }

    @Test
    @DisplayName("constructor: duplicate bureau types throws IllegalStateException")
    void constructor_withDuplicateTypes_throwsException() {
        CreditBureauConnector duplicate = new StubConnector("CIRCULO_DE_CREDITO");

        assertThatThrownBy(() -> new ConnectorRegistry(
                List.of(cdcConnector, duplicate), creditBureauRepository))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("constructor: empty connector list creates empty registry")
    void constructor_withEmptyList_createsEmptyRegistry() {
        registry = new ConnectorRegistry(List.of(), creditBureauRepository);

        assertThat(registry.getSupportedTypes()).isEmpty();
    }

    /**
     * Minimal stub connector for testing registry resolution logic.
     * No external calls — just returns the bureau type.
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
            return CreditReportResult.builder()
                    .success(true)
                    .bureauType(bureauType)
                    .build();
        }

        @Override
        public ConnectionTestResult testConnection(Long creditBureauId) {
            return ConnectionTestResult.builder()
                    .success(true)
                    .bureauType(bureauType)
                    .build();
        }
    }
}
