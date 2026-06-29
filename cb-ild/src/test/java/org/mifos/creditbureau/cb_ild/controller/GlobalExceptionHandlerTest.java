package org.mifos.creditbureau.cb_ild.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.cb_ild.exception.CdcBadRequestException;
import org.mifos.creditbureau.cb_ild.exception.CdcServerException;
import org.mifos.creditbureau.cb_ild.exception.CdcTimeoutException;
import org.mifos.creditbureau.cb_ild.exception.FineractConnectionException;
import org.mifos.creditbureau.cb_ild.exception.FineractNotFoundException;
import org.mifos.creditbureau.cb_ild.exception.FineractServerException;
import org.mifos.creditbureau.cb_ild.exception.KycPrerequisiteException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for GlobalExceptionHandler.
 *
 * Test 1: KycPrerequisiteException → 422 + KYC_PREREQUISITE_FAILED
 * Test 2: CdcBadRequestException → 400 + CDC_BAD_REQUEST
 * Test 3: CdcServerException → 503 + CDC_SERVER_ERROR
 * Test 4: CdcTimeoutException → 503 + CDC_TIMEOUT
 * Test 5: FineractNotFoundException → 404 + FINERACT_CLIENT_NOT_FOUND
 * Test 6: Generic Exception → 500 + INTERNAL_ERROR
 * Test 7: ErrorResponse has non-null timestamp
 * Test 8: ErrorResponse requestId defaults to unknown when MDC empty
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("KycPrerequisiteException → 422 + code KYC_PREREQUISITE_FAILED")
    void handleKycPrerequisite_returns422_withCorrectCode() {
        KycPrerequisiteException ex =
                new KycPrerequisiteException(1L, "nationalId");

        ResponseEntity<ErrorResponse> response =
                handler.handleKycPrerequisite(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code())
                .isEqualTo("KYC_PREREQUISITE_FAILED");
        assertThat(response.getBody().message()).isNotBlank();
    }

    @Test
    @DisplayName("CdcBadRequestException → 400 + code CDC_BAD_REQUEST")
    void handleCdcBadRequest_returns400_withCorrectCode() {
        CdcBadRequestException ex =
                new CdcBadRequestException(1L, "bad data");

        ResponseEntity<ErrorResponse> response =
                handler.handleCdcBadRequest(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().code())
                .isEqualTo("CDC_BAD_REQUEST");
    }

    @Test
    @DisplayName("CdcServerException → 503 + code CDC_SERVER_ERROR")
    void handleCdcServer_returns503_withCorrectCode() {
        CdcServerException ex = new CdcServerException(1L, 503);

        ResponseEntity<ErrorResponse> response =
                handler.handleCdcServer(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code())
                .isEqualTo("CDC_SERVER_ERROR");
    }

    @Test
    @DisplayName("CdcTimeoutException → 503 + code CDC_TIMEOUT")
    void handleCdcTimeout_returns503_withCorrectCode() {
        CdcTimeoutException ex =
                new CdcTimeoutException(1L, "timeout");

        ResponseEntity<ErrorResponse> response =
                handler.handleCdcTimeout(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code())
                .isEqualTo("CDC_TIMEOUT");
    }

    @Test
    @DisplayName("FineractNotFoundException → 404 + FINERACT_CLIENT_NOT_FOUND")
    void handleFineractNotFound_returns404_withCorrectCode() {
        FineractNotFoundException ex =
                new FineractNotFoundException(1L);

        ResponseEntity<ErrorResponse> response =
                handler.handleFineractNotFound(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code())
                .isEqualTo("FINERACT_CLIENT_NOT_FOUND");
    }

    @Test
    @DisplayName("Generic Exception → 500 + INTERNAL_ERROR")
    void handleGeneric_returns500_withInternalError() {
        Exception ex = new RuntimeException("unexpected error");

        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code())
                .isEqualTo("INTERNAL_ERROR");
        // Real exception message never exposed
        assertThat(response.getBody().message())
                .isEqualTo("An unexpected error occurred");
        // message is safe generic phrase — no internal details exposed
    }

    @Test
    @DisplayName("ErrorResponse has non-null timestamp")
    void errorResponse_hasNonNullTimestamp() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("test"));

        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("ErrorResponse requestId defaults to unknown when MDC empty")
    void errorResponse_requestIdDefaultsToUnknown_whenMdcEmpty() {
        ResponseEntity<ErrorResponse> response =
                handler.handleGeneric(new RuntimeException("test"));

        assertThat(response.getBody().requestId()).isEqualTo("unknown");
    }

    @Test
    @DisplayName("FineractConnectionException → 504 + FINERACT_UNREACHABLE")
    void handleFineractConnection_returns504_withCorrectCode() {
        FineractConnectionException ex =
                new FineractConnectionException("Connection timed out");

        ResponseEntity<ErrorResponse> response =
                handler.handleFineractConnection(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(response.getBody().code())
                .isEqualTo("FINERACT_UNREACHABLE");
    }

    @Test
    @DisplayName("FineractServerException → 503 + FINERACT_SERVER_ERROR")
    void handleFineractServer_returns503_withCorrectCode() {
        FineractServerException ex = new FineractServerException(502);

        ResponseEntity<ErrorResponse> response =
                handler.handleFineractServer(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody().code())
                .isEqualTo("FINERACT_SERVER_ERROR");
    }
}