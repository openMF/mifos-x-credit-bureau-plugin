package org.mifos.creditbureau.data;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Wrapper around {@link CBCreditReportData} that includes metadata
 * about the credit report fetch operation.
 */
@Builder
@Getter
public class CreditReportResult {

    /** Whether the credit report was fetched successfully. */
    private final boolean success;

    /** The generalized credit report data. */
    private final CBCreditReportData report;

    /** The bureau type that produced this report (e.g., "CIRCULO_DE_CREDITO"). */
    private final String bureauType;

    /** The raw response body from the external bureau (for auditing/debugging). */
    private final String rawResponse;

    /** Timestamp when the report was fetched. */
    private final Instant fetchedAt;
}
