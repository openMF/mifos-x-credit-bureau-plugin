package org.mifos.creditbureau.cb_ild.repository;

import org.mifos.creditbureau.cb_ild.entity.BureauResponseArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for bureau_response_archive table.
 *
 * Used by RetentionArchiveService — saves expired bureau_response
 * rows to archive before soft-deleting the original.
 *
 * No custom queries needed — JpaRepository.save() is sufficient.
 * Archive rows are never updated or deleted — compliance requirement.
 */
@Repository
public interface BureauResponseArchiveRepository
        extends JpaRepository<BureauResponseArchive, Long> {
}
