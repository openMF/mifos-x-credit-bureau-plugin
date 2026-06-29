package org.mifos.creditbureau.cb_ild.entity.enums;

/**
 * State machine for a dispute raised against a CDC submission's outcome.
 *
 * Allowed transitions, enforced in DisputeServiceImpl (not just by this enum):
 *   OPEN          -> UNDER_REVIEW   (any authenticated role)
 *   UNDER_REVIEW  -> RESOLVED       (COMPLIANCE role only)
 *
 * No other transitions are permitted. Any attempt at a backwards or skipped
 * transition (e.g. RESOLVED -> OPEN, or OPEN -> RESOLVED) must throw
 * IllegalStateException, mapped to HTTP 400 by GlobalExceptionHandler.
 *
 * OPEN          — dispute created. institution_data and cdc_data snapshots taken
 *                  at creation time.
 * UNDER_REVIEW  — a reviewer has picked up the dispute.
 * RESOLVED      — final state. resolved_at is set when entering this state.
 *                  resolution_notes should be populated.
 */
public enum DisputeStatus {
    OPEN,
    UNDER_REVIEW,
    RESOLVED
}
