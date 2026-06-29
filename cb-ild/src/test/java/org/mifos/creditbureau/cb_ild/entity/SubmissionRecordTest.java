package org.mifos.creditbureau.cb_ild.entity;

import org.hibernate.annotations.SQLRestriction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.creditbureau.cb_ild.entity.enums.SubmissionStatus;
import org.mifos.creditbureau.cb_ild.entity.enums.TriggerType;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for SubmissionRecord entity.
 *
 * No Spring context needed — pure reflection + builder tests.
 *
 * Test 1: builder defaults — retryCount=0, softDeleted=false
 * Test 2: @SQLRestriction present with correct value
 * Test 3: V9 columns settable and gettable
 * Test 4: all SubmissionStatus values round-trip via setter/getter
 * Test 5: all TriggerType values round-trip via setter/getter
 * Test 6: full @Builder — all fields set and verified
 */
class SubmissionRecordTest {

    @Test
    @DisplayName("builder defaults — retryCount=0, softDeleted=false")
    void builder_defaults_retryCountZeroSoftDeletedFalse() {
        SubmissionRecord record = SubmissionRecord.builder()
                .clientId(1L)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.PENDING)
                .build();

        assertThat(record.getRetryCount()).isEqualTo(0);
        assertThat(record.getSoftDeleted()).isFalse();
    }

    @Test
    @DisplayName("@SQLRestriction present with soft_deleted = false")
    void sqlRestriction_present_withCorrectValue() {
        SQLRestriction annotation =
                SubmissionRecord.class.getAnnotation(SQLRestriction.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("soft_deleted = false");
    }

    @Test
    @DisplayName("V9 columns settable and gettable")
    void v9Columns_settableAndGettable() {
        LocalDate expiryDate = LocalDate.of(2032, 1, 1);
        LocalDateTime now = LocalDateTime.now();

        SubmissionRecord record = SubmissionRecord.builder()
                .clientId(1L)
                .triggerType(TriggerType.LOAN_APPROVAL)
                .status(SubmissionStatus.ACCEPTED)
                .cdcReferenceId("MOCK-abc-123")
                .inquiryType("HARD")
                .expiryDate(expiryDate)
                .build();

        record.setUpdatedAt(now);

        assertThat(record.getCdcReferenceId()).isEqualTo("MOCK-abc-123");
        assertThat(record.getInquiryType()).isEqualTo("HARD");
        assertThat(record.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(record.getUpdatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("all SubmissionStatus values round-trip via setter/getter")
    void submissionStatus_allValues_roundTrip() {
        SubmissionRecord record = SubmissionRecord.builder()
                .clientId(1L)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.PENDING)
                .build();

        for (SubmissionStatus status : SubmissionStatus.values()) {
            record.setStatus(status);
            assertThat(record.getStatus()).isEqualTo(status);
        }
    }

    @Test
    @DisplayName("all TriggerType values round-trip via setter/getter")
    void triggerType_allValues_roundTrip() {
        SubmissionRecord record = SubmissionRecord.builder()
                .clientId(1L)
                .triggerType(TriggerType.MANUAL_BATCH)
                .status(SubmissionStatus.PENDING)
                .build();

        for (TriggerType trigger : TriggerType.values()) {
            record.setTriggerType(trigger);
            assertThat(record.getTriggerType()).isEqualTo(trigger);
        }
    }

    @Test
    @DisplayName("full builder — all fields explicitly set and verified")
    void builder_allFields_setAndVerified() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate expiryDate = LocalDate.of(2032, 6, 15);

        SubmissionRecord record = SubmissionRecord.builder()
                .id(99L)
                .clientId(42L)
                .loanId(7L)
                .triggerType(TriggerType.LOAN_APPROVAL)
                .status(SubmissionStatus.ACCEPTED)
                .rejectionReason(null)
                .retryCount(0)
                .nextRetryAt(null)
                .submittedAt(now)
                .updatedAt(now)
                .softDeleted(false)
                .cdcReferenceId("MOCK-xyz-999")
                .inquiryType("SOFT")
                .expiryDate(expiryDate)
                .build();

        assertThat(record.getId()).isEqualTo(99L);
        assertThat(record.getClientId()).isEqualTo(42L);
        assertThat(record.getLoanId()).isEqualTo(7L);
        assertThat(record.getTriggerType()).isEqualTo(TriggerType.LOAN_APPROVAL);
        assertThat(record.getStatus()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(record.getRetryCount()).isEqualTo(0);
        assertThat(record.getSoftDeleted()).isFalse();
        assertThat(record.getCdcReferenceId()).isEqualTo("MOCK-xyz-999");
        assertThat(record.getInquiryType()).isEqualTo("SOFT");
        assertThat(record.getExpiryDate()).isEqualTo(expiryDate);
        assertThat(record.getSubmittedAt()).isEqualTo(now);
        assertThat(record.getUpdatedAt()).isEqualTo(now);
    }
}
