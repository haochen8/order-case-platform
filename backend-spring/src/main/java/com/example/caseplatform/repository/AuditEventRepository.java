package com.example.caseplatform.repository;

import com.example.caseplatform.domain.AuditEvent;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    Page<AuditEvent> findByCaseId(UUID caseId, Pageable pageable);
    boolean existsByCaseId(UUID caseId);
}
