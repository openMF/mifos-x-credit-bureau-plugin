package org.mifos.creditbureau.service.connectors.CirculoDeCredito;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mifos.creditbureau.data.CBCreditReportData;
import org.mifos.creditbureau.data.ClientData;
import org.mifos.creditbureau.data.ConnectionTestResult;
import org.mifos.creditbureau.data.CreditReportResult;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoRCCRequest;
import org.mifos.creditbureau.data.creditbureaus.CirculoDeCreditoResponse;
import org.mifos.creditbureau.mappers.CirculoDeCreditoResponseToCBCreditReportDataMapper;
import org.mifos.creditbureau.mappers.ClientDatatoCirculoDeCreditoRccRequest;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CirculoDeCreditoConnector}.
 * <p>
 * This test class is in the same package as the connector to access
 * package-private methods (e.g., {@link SignatureService#buildHeaders}).
 */
@ExtendWith(MockitoExtension.class)
class CirculoDeCreditoConnectorTest {

    @Mock private SignatureService signatureService;
    @Mock private ClientDatatoCirculoDeCreditoRccRequest requestMapper;
    @Mock private CirculoDeCreditoResponseToCBCreditReportDataMapper responseMapper;
    @Mock private RestTemplate restTemplate;

    private CirculoDeCreditoConnector connector;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        connector = new CirculoDeCreditoConnector(
                signatureService, requestMapper, responseMapper,
                objectMapper, restTemplate, "https://test.cdc.mx/");
    }

    @Test
    @DisplayName("getBureauType returns CIRCULO_DE_CREDITO")
    void getBureauType_returnsCIRCULO_DE_CREDITO() {
        assertThat(connector.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
    }

    @Test
    @DisplayName("fetchCreditReport: happy path - calls mapper, signing, HTTP, response mapper in order")
    void fetchCreditReport_happyPath_callsDependenciesInOrder() throws Exception {
        // Arrange
        ClientData clientData = ClientData.builder().build();
        CirculoDeCreditoRCCRequest rccRequest = CirculoDeCreditoRCCRequest.builder().build();
        CBCreditReportData reportData = CBCreditReportData.builder().build();

        when(requestMapper.toCirculoDeCreditoRccRequest(clientData)).thenReturn(rccRequest);
        when(signatureService.buildHeaders(eq(1L), anyString()))
                .thenReturn(Map.of("Authorization", "Bearer test"));
        when(restTemplate.exchange(
                eq("https://test.cdc.mx/v1/rcc"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));
        when(responseMapper.toCBCreditReportData(any(CirculoDeCreditoResponse.class)))
                .thenReturn(reportData);

        // Act
        CreditReportResult result = connector.fetchCreditReport(1L, clientData);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
        assertThat(result.getReport()).isEqualTo(reportData);
        assertThat(result.getRawResponse()).isEqualTo("{}");
        assertThat(result.getFetchedAt()).isNotNull();

        // Verify call order: map → sign → HTTP → response map
        InOrder order = inOrder(requestMapper, signatureService, restTemplate, responseMapper);
        order.verify(requestMapper).toCirculoDeCreditoRccRequest(clientData);
        order.verify(signatureService).buildHeaders(eq(1L), anyString());
        order.verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class));
        order.verify(responseMapper).toCBCreditReportData(any(CirculoDeCreditoResponse.class));
    }

    @Test
    @DisplayName("fetchCreditReport: HTTP 500 from bureau throws RuntimeException")
    void fetchCreditReport_httpServerError_throwsException() throws Exception {
        ClientData clientData = ClientData.builder().build();
        CirculoDeCreditoRCCRequest rccRequest = CirculoDeCreditoRCCRequest.builder().build();

        when(requestMapper.toCirculoDeCreditoRccRequest(clientData)).thenReturn(rccRequest);
        when(signatureService.buildHeaders(eq(1L), anyString()))
                .thenReturn(Map.of("Authorization", "Bearer test"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> connector.fetchCreditReport(1L, clientData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Credit bureau unavailable");
    }

    @Test
    @DisplayName("fetchCreditReport: connection timeout throws RuntimeException")
    void fetchCreditReport_timeout_throwsException() throws Exception {
        ClientData clientData = ClientData.builder().build();
        CirculoDeCreditoRCCRequest rccRequest = CirculoDeCreditoRCCRequest.builder().build();

        when(requestMapper.toCirculoDeCreditoRccRequest(clientData)).thenReturn(rccRequest);
        when(signatureService.buildHeaders(eq(1L), anyString()))
                .thenReturn(Map.of("Authorization", "Bearer test"));
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST),
                any(HttpEntity.class), eq(String.class)))
                .thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> connector.fetchCreditReport(1L, clientData))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Unable to reach credit bureau");
    }

    @Test
    @DisplayName("testConnection: success returns ConnectionTestResult with success=true")
    void testConnection_success_returnsSuccessResult() throws Exception {
        when(signatureService.buildHeaders(eq(1L), anyString()))
                .thenReturn(Map.of("Authorization", "Bearer test"));
        when(restTemplate.exchange(
                eq("https://test.cdc.mx/v1/securitytest"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        ConnectionTestResult result = connector.testConnection(1L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
        assertThat(result.getMessage()).isEqualTo("OK");
        assertThat(result.getHttpStatusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("testConnection: failure returns result with success=false, not exception")
    void testConnection_failure_returnsFailureResult() throws Exception {
        when(signatureService.buildHeaders(eq(1L), anyString()))
                .thenThrow(new RuntimeException("Key decryption failed"));

        ConnectionTestResult result = connector.testConnection(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getBureauType()).isEqualTo("CIRCULO_DE_CREDITO");
        assertThat(result.getMessage()).contains("Key decryption failed");
        assertThat(result.getHttpStatusCode()).isEqualTo(0);
    }
}
