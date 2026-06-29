package org.mifos.creditbureau.cb_ild.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for AuditEntry entity.
 *
 * Test 1: Builder stores all fields correctly
 * Test 2: result SUCCESS stored correctly
 * Test 3: result FAILURE stored correctly
 * Test 4: errorMessage null allowed
 * Test 5: No softDeleted field — audit never deleted
 */
class AuditEntryTest {

    @Test
    @DisplayName("Builder stores all fields correctly")
    void builder_allFields_storedCorrectly() {
        AuditEntry entry = AuditEntry.builder()
                .action("CDC_SCORE_PULL")
                .entityType("BureauResponse")
                .performedBy("user123")
                .requestId("req-abc-123")
                .durationMs(250L)
                .result("SUCCESS")
                .errorMessage(null)
                .build();

        assertThat(entry.getAction()).isEqualTo("CDC_SCORE_PULL");
        assertThat(entry.getEntityType()).isEqualTo("BureauResponse");
        assertThat(entry.getPerformedBy()).isEqualTo("user123");
        assertThat(entry.getRequestId()).isEqualTo("req-abc-123");
        assertThat(entry.getDurationMs()).isEqualTo(250L);
        assertThat(entry.getResult()).isEqualTo("SUCCESS");
        assertThat(entry.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("result SUCCESS stored correctly")
    void builder_resultSuccess_storedCorrectly() {
        AuditEntry entry = AuditEntry.builder()
                .action("CDC_SCORE_PULL")
                .result("SUCCESS")
                .build();

        assertThat(entry.getResult()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("result FAILURE with errorMessage stored correctly")
    void builder_resultFailure_withErrorMessage() {
        AuditEntry entry = AuditEntry.builder()
                .action("CDC_SCORE_PULL")
                .result("FAILURE")
                .errorMessage("KYC prerequisite not met")
                .build();

        assertThat(entry.getResult()).isEqualTo("FAILURE");
        assertThat(entry.getErrorMessage())
                .isEqualTo("KYC prerequisite not met");
    }

    @Test
    @DisplayName("errorMessage null — valid for SUCCESS entries")
    void builder_errorMessageNull_validForSuccess() {
        AuditEntry entry = AuditEntry.builder()
                .action("CDC_SCORE_PULL")
                .result("SUCCESS")
                .errorMessage(null)
                .build();

        assertThat(entry.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("performedBy anonymous — valid when unauthenticated")
    void builder_performedByAnonymous_validWhenUnauthenticated() {
        AuditEntry entry = AuditEntry.builder()
                .action("CDC_SCORE_PULL")
                .performedBy("anonymous")
                .build();

        assertThat(entry.getPerformedBy()).isEqualTo("anonymous");
    }
}
