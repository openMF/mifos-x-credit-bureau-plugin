package org.mifos.creditbureau.cb_ild.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mifos.creditbureau.cb_ild.exception.FineractConnectionException;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.exception.FineractServerException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FineractApiClient.
 *
 * Test 1:  Happy path — all 3 endpoints return data
 * Test 2:  /identifiers returns [] → externalId fallback, no exception
 * Test 3:  /identifiers has NATIONAL_ID → RFC extracted from documentKey
 * Test 4:  /addresses returns 404 → null returned, no exception (Bug 3 fix)
 * Test 5:  /clients/{id} returns 404 → FineractNotFoundException
 * Test 6:  /clients/{id} returns 500 → FineractServerException
 * Test 7:  Connection timeout → FineractConnectionException
 * Test 8:  Multiple identifiers, NATIONAL_ID second → correct RFC extracted
 * Test 9:  clientId null → IllegalArgumentException
 * Test 10: Identifiers present but none NATIONAL_ID → externalId fallback
 * Test 11: baseUrl with trailing slash → slash stripped, URL correct
 * Test 12: validateRfc with blank string → nationalId null
 * Test 13: /addresses returns 5xx → null returned, no exception
 * Test 14: /identifiers returns 4xx → externalId fallback, no exception
 *
 * Pure unit tests — no Spring context, no real HTTP calls.
 * FineractApiClient constructed manually in @BeforeEach.
 * Exact URL matching prevents stub routing conflicts.
 */
@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class FineractApiClientTest {

    @Mock
    private RestTemplate restTemplate;

    private FineractApiClient client;

    private static final String BASE_URL =
            "http://localhost:8099/fineract-provider/api/v1";
    private static final String TENANT_ID = "default";
    private static final Long CLIENT_ID = 1L;
    private static final String TEST_RFC = "ABCD123456";

    // Exact URLs — prevents stub matching conflicts between 3 endpoints
    private static final String BASIC_URL =
            BASE_URL + "/clients/" + CLIENT_ID;
    private static final String IDENTIFIERS_URL =
            BASE_URL + "/clients/" + CLIENT_ID + "/identifiers";
    private static final String ADDRESSES_URL =
            BASE_URL + "/clients/" + CLIENT_ID + "/addresses";

    @BeforeEach
    void setUp() {
        // Construct manually — @Value and @Qualifier not available in unit tests
        client = new FineractApiClient(restTemplate, BASE_URL, TENANT_ID);
    }

    // ===== TEST 1 — Happy path =====

    @Test
    @DisplayName("All 3 endpoints return data — FineractClientData fully populated")
    void getClientData_allEndpointsReturnData_returnsFullyPopulatedClientData() {
        stubBasicClient(basicClient("EXT123"));
        stubIdentifiers(List.of(nationalIdIdentifier(TEST_RFC)));
        stubAddresses(List.of(validAddress()));

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.clientId()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("Juan");
        assertThat(result.lastName()).isEqualTo("Prueba");
        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
        assertThat(result.phoneNumber()).isEqualTo("5512345678");
        assertThat(result.emailAddress()).isEqualTo("juan@test.com");
        assertThat(result.hasDob()).isTrue();
        assertThat(result.hasAddress()).isTrue();
        assertThat(result.hasNationalId()).isTrue();
    }

    // ===== TEST 2 — /identifiers returns empty array =====

    @Test
    @DisplayName("/identifiers returns [] — falls back to externalId, no exception")
    void getClientData_identifiersEmpty_fallsBackToExternalId() {
        stubBasicClient(basicClient(TEST_RFC));
        stubIdentifiers(List.of());
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
    }

    // ===== TEST 3 — /identifiers has NATIONAL_ID =====

    @Test
    @DisplayName("/identifiers has NATIONAL_ID — RFC extracted from documentKey")
    void getClientData_identifiersHasNationalId_extractsRfcFromDocumentKey() {
        stubBasicClient(basicClient(null));
        stubIdentifiers(List.of(nationalIdIdentifier(TEST_RFC)));
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
        assertThat(result.hasNationalId()).isTrue();
    }

    // ===== TEST 4 — /addresses returns 404 =====

    @Test
    @DisplayName("/addresses returns 404 — address null, no exception thrown")
    void getClientData_addresses404_returnsNullAddressFields() {
        stubBasicClient(basicClient(TEST_RFC));
        stubIdentifiers(List.of());
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.addressLine1()).isNull();
        assertThat(result.city()).isNull();
        assertThat(result.hasAddress()).isFalse();
    }

    // ===== TEST 5 — /clients returns 404 =====

    @Test
    @DisplayName("/clients/{id} returns 404 — FineractNotFoundException thrown")
    void getClientData_basicClient404_throwsFineractNotFoundException() {
        when(restTemplate.exchange(
                eq(BASIC_URL),
                eq(HttpMethod.GET),
                any(),
                eq(FineractClientResponse.class)
        )).thenThrow(HttpClientErrorException.NotFound.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null
        ));

        assertThatThrownBy(() -> client.getClientData(CLIENT_ID))
                .isInstanceOf(FineractNotFoundException.class);
    }

    // ===== TEST 6 — /clients returns 500 =====

    @Test
    @DisplayName("/clients/{id} returns 500 — FineractServerException thrown")
    void getClientData_basicClient500_throwsFineractServerException() {
        when(restTemplate.exchange(
                eq(BASIC_URL),
                eq(HttpMethod.GET),
                any(),
                eq(FineractClientResponse.class)
        )).thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error", null, null, null
        ));

        assertThatThrownBy(() -> client.getClientData(CLIENT_ID))
                .isInstanceOf(FineractServerException.class);
    }

    // ===== TEST 7 — Connection timeout =====

    @Test
    @DisplayName("Connection timeout — FineractConnectionException thrown")
    void getClientData_connectionTimeout_throwsFineractConnectionException() {
        when(restTemplate.exchange(
                eq(BASIC_URL),
                eq(HttpMethod.GET),
                any(),
                eq(FineractClientResponse.class)
        )).thenThrow(new ResourceAccessException("Connection timed out"));

        assertThatThrownBy(() -> client.getClientData(CLIENT_ID))
                .isInstanceOf(FineractConnectionException.class);
    }

    // ===== TEST 8 — Multiple identifiers, NATIONAL_ID is second =====

    @Test
    @DisplayName("Multiple identifiers — NATIONAL_ID second — correct RFC extracted")
    void getClientData_multipleIdentifiers_nationalIdSecond_extractsCorrectRfc() {
        FineractIdentifierDTO passport = new FineractIdentifierDTO(
                new FineractIdentifierDTO.DocumentType("PASSPORT"),
                "PASSPORT123"
        );
        FineractIdentifierDTO nationalId = nationalIdIdentifier(TEST_RFC);

        stubBasicClient(basicClient("FALLBACK_RFC"));
        stubIdentifiers(List.of(passport, nationalId));
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
        assertThat(result.nationalId()).isNotEqualTo("PASSPORT123");
        assertThat(result.nationalId()).isNotEqualTo("FALLBACK_RFC");
    }

    // ===== TEST 9 — Null clientId =====

    @Test
    @DisplayName("clientId null — IllegalArgumentException thrown immediately")
    void getClientData_nullClientId_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> client.getClientData(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("clientId must not be null");
    }

    // ===== TEST 10 — Identifiers present but none NATIONAL_ID =====

    @Test
    @DisplayName("Identifiers present but none NATIONAL_ID — falls back to externalId")
    void getClientData_identifiersNoNationalId_fallsBackToExternalId() {
        FineractIdentifierDTO passport = new FineractIdentifierDTO(
                new FineractIdentifierDTO.DocumentType("PASSPORT"),
                "PASSPORT123"
        );

        stubBasicClient(basicClient(TEST_RFC));
        stubIdentifiers(List.of(passport));
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
        assertThat(result.nationalId()).isNotEqualTo("PASSPORT123");
    }

    // ===== TEST 11 — Constructor strips trailing slash =====

    @Test
    @DisplayName("baseUrl with trailing slash — slash stripped, URLs built correctly")
    void constructor_baseUrlWithTrailingSlash_slashStripped() {
        // Create client with trailing slash
        FineractApiClient clientWithSlash = new FineractApiClient(
                restTemplate, BASE_URL + "/", TENANT_ID
        );

        // Stub with exact URL — would fail if double-slash present
        when(restTemplate.exchange(
                eq(BASIC_URL),
                eq(HttpMethod.GET),
                any(),
                eq(FineractClientResponse.class)
        )).thenReturn(ResponseEntity.ok(basicClient(TEST_RFC)));

        when(restTemplate.exchange(
                eq(IDENTIFIERS_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(List.of()));

        when(restTemplate.exchange(
                eq(ADDRESSES_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.NotFound.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null
        ));

        // Must not throw — confirms slash was stripped correctly
        FineractClientData result = clientWithSlash.getClientData(CLIENT_ID);
        assertThat(result).isNotNull();
    }

    // ===== TEST 12 — Blank string RFC =====

    @Test
    @DisplayName("externalId is blank string — nationalId null, not blank RFC")
    void getClientData_externalIdBlank_nationalIdNull() {
        // blank externalId — validateRfc must return null not " "
        stubBasicClient(basicClient("   "));
        stubIdentifiers(List.of());
        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result.nationalId()).isNull();
        assertThat(result.hasNationalId()).isFalse();
    }

    // ===== TEST 13 — /addresses returns 5xx =====

    @Test
    @DisplayName("/addresses returns 5xx — address null, no exception thrown")
    void getClientData_addresses5xx_returnsNullAddress() {
        stubBasicClient(basicClient(TEST_RFC));
        stubIdentifiers(List.of());

        when(restTemplate.exchange(
                eq(ADDRESSES_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Server Error", null, null, null
        ));

        // Must NOT throw
        FineractClientData result = client.getClientData(CLIENT_ID);

        assertThat(result).isNotNull();
        assertThat(result.addressLine1()).isNull();
        assertThat(result.hasAddress()).isFalse();
    }

    // ===== TEST 14 — /identifiers returns 4xx =====

    @Test
    @DisplayName("/identifiers returns 4xx — falls back to externalId, no exception")
    void getClientData_identifiers4xx_fallsBackToExternalId() {
        stubBasicClient(basicClient(TEST_RFC));

        when(restTemplate.exchange(
                eq(IDENTIFIERS_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(
                HttpStatus.FORBIDDEN, "Forbidden", null, null, null
        ));

        stubAddresses404();

        FineractClientData result = client.getClientData(CLIENT_ID);

        // Falls back to externalId
        assertThat(result.nationalId()).isEqualTo(TEST_RFC);
    }

    // ===== HELPERS =====

    private FineractClientResponse basicClient(String externalId) {
        return new FineractClientResponse(
                1L, "Juan", "Prueba", externalId,
                "5512345678", "juan@test.com", List.of(1990, 5, 15)
        );
    }

    private FineractIdentifierDTO nationalIdIdentifier(String rfc) {
        return new FineractIdentifierDTO(
                new FineractIdentifierDTO.DocumentType("NATIONAL_ID"),
                rfc
        );
    }

    private FineractAddressDTO validAddress() {
        return new FineractAddressDTO(
                1L, "HOME", "Insurgentes",
                "Insurgentes Sur 1007", null, null, null,
                "Mexico City", null, "Mexico", "CDMX", "11230"
        );
    }

    private void stubBasicClient(FineractClientResponse response) {
        when(restTemplate.exchange(
                eq(BASIC_URL),
                eq(HttpMethod.GET),
                any(),
                eq(FineractClientResponse.class)
        )).thenReturn(ResponseEntity.ok(response));
    }

    private void stubIdentifiers(List<FineractIdentifierDTO> identifiers) {
        when(restTemplate.exchange(
                eq(IDENTIFIERS_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(identifiers));
    }

    private void stubAddresses(List<FineractAddressDTO> addresses) {
        when(restTemplate.exchange(
                eq(ADDRESSES_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(addresses));
    }

    private void stubAddresses404() {
        when(restTemplate.exchange(
                eq(ADDRESSES_URL),
                eq(HttpMethod.GET),
                any(),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.NotFound.create(
                HttpStatus.NOT_FOUND, "Not Found", null, null, null
        ));
    }
}
