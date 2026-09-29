package com.example.caseplatform.repository;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.domain.enums.CaseStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaseRepository extends JpaRepository<Case, UUID> {

    // Serialize mutations of a case and its orders to enforce cross-row invariants.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Case c WHERE c.id = :id")
    Optional<Case> findForUpdate(@Param("id") UUID id);

    @Query("""
            SELECT c FROM Case c
            WHERE (:status IS NULL OR c.status = :status)
              AND (
                CAST(:search AS string) IS NULL
                OR LOWER(c.title) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                OR LOWER(COALESCE(c.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<Case> findByFilters(
            @Param("status") CaseStatus status,
            @Param("search") String search,
            Pageable pageable);
}
