package com.example.caseplatform.service;

import com.example.caseplatform.domain.AuditEvent;
import com.example.caseplatform.dto.AuditEventResponse;
import com.example.caseplatform.exception.ResourceNotFoundException;
import com.example.caseplatform.repository.AuditEventRepository;
import com.example.caseplatform.repository.CaseRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AuditEventRepository events;
    private final CaseRepository cases;

    public AuditService(AuditEventRepository events, CaseRepository cases) {
        this.events = events;
        this.cases = cases;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(UUID caseId, String entityType, UUID entityId, String eventType,
                       String actor, Map<String, Object> payload) {
        events.save(new AuditEvent(caseId, entityType, entityId, eventType, actor, payload));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> history(UUID caseId, Pageable pageable) {
        if (!cases.existsById(caseId) && !events.existsByCaseId(caseId)) {
            throw new ResourceNotFoundException("Case not found: " + caseId);
        }
        return events.findByCaseId(caseId, pageable).map(AuditEventResponse::from);
    }
}
