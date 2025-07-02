package org.mifos.creditbureau.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link CreditBureauOrganization} entities.
 */
@Repository
public interface CreditBureauOrganizationRepository extends JpaRepository<CreditBureauOrganization, Long> {
    
    /**
     * Find a credit bureau organization by its name.
     *
     * @param name the name of the credit bureau organization
     * @return an Optional containing the found organization or empty if not found
     */
    Optional<CreditBureauOrganization> findByCreditBureauName(String name);
    
    /**
     * Find all active credit bureau organizations.
     *
     * @return a list of active credit bureau organizations
     */
    List<CreditBureauOrganization> findByCreditBureauOrganizationIsActiveTrue();
    
    /**
     * Find all available credit bureau organizations.
     *
     * @return a list of available credit bureau organizations
     */
    List<CreditBureauOrganization> findByCreditBureauOrganizationIsAvailableTrue();
    
    /**
     * Find all credit bureau organizations for a specific country.
     *
     * @param country the country to search for
     * @return a list of credit bureau organizations for the specified country
     */
    List<CreditBureauOrganization> findByCountry(String country);
}