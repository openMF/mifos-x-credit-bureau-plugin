package org.mifos.creditbureau.cb_ild.service.dispute;

import org.mifos.creditbureau.cb_ild.entity.DisputeCase;

/**
 * Interface for the dispute workflow (MX-275).
 *
 * State machine (enforced in DisputeServiceImpl):
 *   OPEN -> UNDER_REVIEW -> RESOLVED
 *   No backwards or skipped transitions allowed.
 */
public interface IDisputeService {

    /**
     * Raises a new dispute against a CDC submission outcome.
     *
     * @param submissionRecordId ID of the SubmissionRecord being disputed
     * @param disputeDetails     free-text description of what is disputed
     * @param raisedBy           userId of who is raising the dispute
     * @return the saved DisputeCase in OPEN status
     * @throws IllegalArgumentException if any param is null or blank,
     *         or if submissionRecordId does not exist
     */
    DisputeCase createDispute(
            Long submissionRecordId,
            String disputeDetails,
            String raisedBy);

    /**
     * Moves a dispute forward in the state machine.
     *
     * Allowed: OPEN->UNDER_REVIEW, UNDER_REVIEW->RESOLVED.
     * Forbidden (IllegalStateException -> 400):
     *   OPEN->RESOLVED, UNDER_REVIEW->OPEN, RESOLVED->anything.
     *
     * @param disputeId       ID of the DisputeCase to update
     * @param newStatus       target status string
     * @param resolutionNotes required when newStatus is RESOLVED
     * @return the updated DisputeCase
     * @throws IllegalArgumentException if disputeId or newStatus is null,
     *         or if dispute does not exist
     * @throws IllegalStateException if the transition is not allowed
     */
    DisputeCase updateStatus(
            Long disputeId,
            String newStatus,
            String resolutionNotes);
}
