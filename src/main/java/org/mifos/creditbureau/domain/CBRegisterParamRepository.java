package org.mifos.creditbureau.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link CBRegisterParams} entities.
 */
@Repository
public interface CBRegisterParamRepository extends JpaRepository<CBRegisterParams, Long> {

    Optional<CBRegisterParams> findById(Long creditBureauId);



}