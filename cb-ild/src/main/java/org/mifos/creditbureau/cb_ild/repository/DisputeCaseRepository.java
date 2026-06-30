package org.mifos.creditbureau.cb_ild.repository;

import org.mifos.creditbureau.cb_ild.entity.DisputeCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for dispute_case table.
 *
 * @SQLRestriction on DisputeCase ensures soft_deleted=false
 * automatically on every query — no manual filter needed here.
 *
 * Used by:
 *   DisputeServiceImpl (MX-275)
 *   Angular Tab 4 — POST/PUT /api/disputes, GET /api/disputes/{id}
 *
 * findById(Long id) is inherited from JpaRepository and covers
 * GET /api/disputes/{id} directly — no custom method needed for that.
 */
@Repository
public interface DisputeCaseRepository
        extends JpaRepository<DisputeCase, Long> {

    /**
     * All non-deleted disputes already raised against a given submission.
     *
     * Used by:
     *   DisputeServiceImpl.createDispute() — optional duplicate check before
     *   opening a new dispute for the same submissionRecordId. Not enforced
     *   by a DB constraint (a submission could legitimately have multiple
     *   disputes over time, e.g. one resolved and a new one raised later),
     *   so this is a lookup for the service layer to use at its discretion,
     *   not a hard uniqueness rule.
     *
     * SQL: SELECT * FROM dispute_case
     *      WHERE submission_record_id = ? AND soft_deleted = false
     */
    List<DisputeCase>
        findBySubmissionRecordId(Long submissionRecordId);
}
