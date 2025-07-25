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
         // By extending JpaRepository, you automatically get methods like:
                 // - save(CreditCheck entity)
                 // - findById(Long id)
                 // - findAll()
                 // - deleteById(Long id)

                 // You can also define custom query methods here if needed, e.g.:
                 // List<CreditCheck> findByStatus(String status);

}