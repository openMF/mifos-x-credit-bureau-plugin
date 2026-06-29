package org.mifos.creditbureau.cb_ild.entity.enums;

/**
 * Identifies how a CDC submission was triggered.
 *
 * LOAN_APPROVAL    — Trigger 2: fired after a loan officer approves a loan in Fineract.
 * SCREENING_EVENT  — Trigger 3: fired during a KYC screening event (LRSIC inquiry log).
 * MANUAL_BATCH     — fired by the scheduled retry batch or a manual run via
 *                     POST /api/submissions/run.
 */
public enum TriggerType {
    LOAN_APPROVAL,
    SCREENING_EVENT,
    MANUAL_BATCH
}
