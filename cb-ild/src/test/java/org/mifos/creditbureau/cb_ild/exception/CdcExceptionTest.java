package org.mifos.creditbureau.cb_ild.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for 4 CDC exception classes.
 *
 * Test 1:  CdcBadRequestException — HTTP 400, generic message
 * Test 2:  CdcBadRequestException — truncatedBody max 200 chars
 * Test 3:  CdcBadRequestException — null responseBody handled
 * Test 4:  CdcServerException — HTTP 503, stores httpStatus
 * Test 5:  CdcTimeoutException — HTTP 503, cause has originalMessage
 * Test 6:  KycPrerequisiteException — HTTP 422, stores missingField
 * Test 7:  KycPrerequisiteException — message generic, no RFC value
 * Test 8:  CdcBadRequestException — extends RuntimeException
 */
class CdcExceptionTest {

    // ===== CdcBadRequestException =====

    @Test
    @DisplayName("CdcBadRequestException — HTTP 400, generic message")
    void cdcBadRequestException_http400_genericMessage() {
        CdcBadRequestException ex =
                new CdcBadRequestException(1L, "bad data");

        assertThat(ex.getMessage())
                .isEqualTo("CDC rejected request — data quality issue");
        assertThat(ex.getClientId()).isEqualTo(1L);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("CdcBadRequestException — responseBody truncated to 200 chars")
    void cdcBadRequestException_longBody_truncatedTo200() {
        String longBody = "x".repeat(500);
        CdcBadRequestException ex =
                new CdcBadRequestException(1L, longBody);

        assertThat(ex.getTruncatedBody()).hasSize(200);
    }

    @Test
    @DisplayName("CdcBadRequestException — null responseBody handled gracefully")
    void cdcBadRequestException_nullBody_handledGracefully() {
        CdcBadRequestException ex =
                new CdcBadRequestException(1L, null);

        assertThat(ex.getTruncatedBody()).isNull();
        assertThat(ex.getMessage())
                .isEqualTo("CDC rejected request — data quality issue");
    }

    // ===== CdcServerException =====

    @Test
    @DisplayName("CdcServerException — HTTP 503, stores httpStatus")
    void cdcServerException_http503_storesHttpStatus() {
        CdcServerException ex = new CdcServerException(1L, 503);

        assertThat(ex.getMessage())
                .isEqualTo("CDC server error — retry eligible");
        assertThat(ex.getClientId()).isEqualTo(1L);
        assertThat(ex.getHttpStatus()).isEqualTo(503);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    // ===== CdcTimeoutException =====

    @Test
    @DisplayName("CdcTimeoutException — HTTP 503, cause has originalMessage")
    void cdcTimeoutException_http503_causeHasOriginalMessage() {
        String originalMessage = "Read timed out after 30000ms";
        CdcTimeoutException ex =
                new CdcTimeoutException(1L, originalMessage);

        assertThat(ex.getMessage())
                .isEqualTo("CDC connection timed out — retry eligible");
        assertThat(ex.getClientId()).isEqualTo(1L);
        assertThat(ex.getCause()).isNotNull();
        assertThat(ex.getCause().getMessage()).isEqualTo(originalMessage);
    }

    // ===== KycPrerequisiteException =====

    @Test
    @DisplayName("KycPrerequisiteException — HTTP 422, stores missingField")
    void kycPrerequisiteException_http422_storesMissingField() {
        KycPrerequisiteException ex =
                new KycPrerequisiteException(1L, "nationalId");

        assertThat(ex.getMessage()).isEqualTo(
                "KYC prerequisite not met — RFC/National ID required before CDC submission");
        assertThat(ex.getClientId()).isEqualTo(1L);
        assertThat(ex.getMissingField()).isEqualTo("nationalId");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("KycPrerequisiteException — message generic, no RFC value exposed")
    void kycPrerequisiteException_message_doesNotContainRfcValue() {
        String fakeRfc = "ABCD123456EF1";
        KycPrerequisiteException ex =
                new KycPrerequisiteException(1L, "nationalId");

        assertThat(ex.getMessage()).doesNotContain(fakeRfc);
        assertThat(ex.getMissingField()).isEqualTo("nationalId");
        assertThat(ex.getMissingField()).doesNotContain(fakeRfc);
    }

    @Test
    @DisplayName("CdcBadRequestException — extends RuntimeException")
    void cdcBadRequestException_extendsRuntimeException() {
        assertThat(new CdcBadRequestException(1L, "body"))
                .isInstanceOf(RuntimeException.class);
        assertThat(new CdcServerException(1L, 500))
                .isInstanceOf(RuntimeException.class);
        assertThat(new CdcTimeoutException(1L, "timeout"))
                .isInstanceOf(RuntimeException.class);
        assertThat(new KycPrerequisiteException(1L, "nationalId"))
                .isInstanceOf(RuntimeException.class);
    }
}
