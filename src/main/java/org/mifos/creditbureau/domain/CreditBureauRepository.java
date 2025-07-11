package org.mifos.creditbureau.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing {@link CreditBureau} entities.
 */
@Repository
public interface CreditBureauRepository extends JpaRepository<CreditBureau, Long> {
    

    Optional<CreditBureau> findByCreditBureauName(String name);

    List<CreditBureau> findByIsActiveTrue();

    List<CreditBureau> findByIsAvailableTrue();

    List<CreditBureau> findByCountry(String country);
}