package org.mifos.creditbureau.cb_ild.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for all 3 Fineract exception classes.
 *
 * Test 1: FineractNotFoundException stores clientId, generic message
 * Test 2: FineractNotFoundException accepts null clientId
 * Test 3: FineractServerException stores httpStatus, generic message
 * Test 4: FineractServerException stores 200 for empty body case
 * Test 5: FineractConnectionException stores cause, generic external message
 */
class FineractExceptionTest {

    @Test
    @DisplayName("FineractNotFoundException — stores clientId, returns generic message")
    void fineractNotFoundException_storesClientId_returnsGenericMessage() {
        FineractNotFoundException ex = new FineractNotFoundException(42L);

        assertThat(ex.getClientId()).isEqualTo(42L);
        assertThat(ex.getMessage()).isEqualTo("Client not found in Fineract");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("FineractNotFoundException — null clientId accepted")
    void fineractNotFoundException_nullClientId_accepted() {
        FineractNotFoundException ex = new FineractNotFoundException(null);

        assertThat(ex.getClientId()).isNull();
        assertThat(ex.getMessage()).isEqualTo("Client not found in Fineract");
    }

    @Test
    @DisplayName("FineractServerException — stores httpStatus, returns generic message")
    void fineractServerException_storesHttpStatus_returnsGenericMessage() {
        FineractServerException ex = new FineractServerException(500);

        assertThat(ex.getHttpStatus()).isEqualTo(500);
        assertThat(ex.getMessage()).isEqualTo("Fineract server error — retry eligible");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("FineractServerException — stores 200 for empty body case")
    void fineractServerException_status200_storedCorrectly() {
        FineractServerException ex = new FineractServerException(200);

        assertThat(ex.getHttpStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("FineractConnectionException — cause has original message, external message generic")
    void fineractConnectionException_causeHasOriginalMessage_externalMessageGeneric() {
        String originalMessage = "Connection refused: localhost/127.0.0.1:8099";
        FineractConnectionException ex =
                new FineractConnectionException(originalMessage);

        assertThat(ex.getMessage())
                .isEqualTo("Could not connect to Fineract — retry eligible");
        assertThat(ex.getCause()).isNotNull();
        assertThat(ex.getCause().getMessage()).isEqualTo(originalMessage);
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }
}
