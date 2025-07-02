package org.mifos.creditbureau.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link CreditBureauRegistrationParam} entities.
 */
@Repository
public interface CreditBureauRegistrationParamRepository extends JpaRepository<CreditBureauRegistrationParam, Long> {
    
    /**
     * Find registration parameters by credit bureau organization ID.
     *
     * @param creditBureauOrganizationId the ID of the credit bureau organization
     * @return a list of registration parameters for the specified organization
     */
    List<CreditBureauRegistrationParam> findByCreditBureauOrganizationId(Long creditBureauOrganizationId);
    
    /**
     * Find a specific registration parameter by credit bureau organization ID and parameter ID.
     *
     * @param creditBureauOrganizationId the ID of the credit bureau organization
     * @param creditBureauOrganizationParamId the ID of the registration parameter
     * @return an Optional containing the found parameter or empty if not found
     */
    Optional<CreditBureauRegistrationParam> findByCreditBureauOrganizationIdAndCreditBureauOrganizationParamId(
            Long creditBureauOrganizationId, Long creditBureauOrganizationParamId);
}