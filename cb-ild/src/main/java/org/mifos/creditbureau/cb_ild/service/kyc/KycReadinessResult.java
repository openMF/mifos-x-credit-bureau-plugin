package org.mifos.creditbureau.cb_ild.service.kyc;

import java.time.Instant;
import java.util.List;

/**
 * DTO returned by GET /api/clients/{id}/bureau-readiness.
 * Angular Tab 1 receives this exactly.
 *
 * Java 21 record — immutable, compact, cannot be modified after creation.
 *
 * Security — fields NEVER in this record:
 *   nationalId       — RFC is PII
 *   dateOfBirth      — PII
 *   rawResponseHash  — internal audit only
 *   fullResponse     — raw CDC data, PII
 *   softDeleted      — internal flag
 */
public record KycReadinessResult(
        long clientId,
        int score,
        boolean ready,
        List<String> missingFields,
        Integer ficoScore,
        String riskBand,
        boolean scoreDropAlert,
        Instant pulledAt
) {}
