package com.example.caseplatform.service;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.domain.enums.CaseStatus;
import com.example.caseplatform.dto.AuditEventRequest;
import com.example.caseplatform.dto.CaseCreateRequest;
import com.example.caseplatform.dto.CaseResponse;
import com.example.caseplatform.dto.CaseUpdateRequest;
import com.example.caseplatform.exception.BadRequestException;
import com.example.caseplatform.exception.ResourceNotFoundException;
import com.example.caseplatform.mapper.CaseMapper;
import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final OrderRepository orderRepository;
    private final CaseMapper caseMapper;
    private final AuditClient auditClient;

    public CaseService(
            CaseRepository caseRepository,
            OrderRepository orderRepository,
            CaseMapper caseMapper,
            AuditClient auditClient) {
        this.caseRepository = caseRepository;
        this.orderRepository = orderRepository;
        this.caseMapper = caseMapper;
        this.auditClient = auditClient;
    }

    @Transactional(readOnly = true)
    public Page<CaseResponse> getCases(CaseStatus status, String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        return caseRepository.findByFilters(status, normalizedSearch, pageable).map(caseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID id) {
        return caseMapper.toResponse(findCaseById(id));
    }

    @Transactional
    public CaseResponse createCase(CaseCreateRequest request, String actor) {
        Case toCreate = new Case();
        toCreate.setTitle(request.getTitle().trim());
        toCreate.setDescription(request.getDescription());
        toCreate.setStatus(CaseStatus.OPEN);

        Case created = caseRepository.save(toCreate);
        publishCaseEvent("CASE_CREATED", created.getId(), actor, Map.of("status", created.getStatus().name()));
        return caseMapper.toResponse(created);
    }

    @Transactional
    public CaseResponse updateCase(UUID id, CaseUpdateRequest request, String actor) {
        Case existing = findCaseById(id);

        if (StringUtils.hasText(request.getTitle())) {
            existing.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        applyStatusTransition(existing, request.getStatus());

        Case updated = caseRepository.save(existing);
        publishCaseEvent("CASE_UPDATED", updated.getId(), actor, Map.of("status", updated.getStatus().name()));
        return caseMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCase(UUID id, String actor) {
        Case existing = findCaseById(id);
        orderRepository.deleteByCaseId(id);
        caseRepository.delete(existing);
        publishCaseEvent("CASE_DELETED", id, actor, Map.of("status", existing.getStatus().name()));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getCaseAudit(UUID id) {
        findCaseById(id);
        return auditClient.getEvents("CASE", id);
    }

    private Case findCaseById(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
    }

    private void publishCaseEvent(String eventType, UUID caseId, String actor, Map<String, Object> payload) {
        AuditEventRequest event = new AuditEventRequest();
        event.setEventType(eventType);
        event.setEntityType("CASE");
        event.setEntityId(caseId.toString());
        event.setActor(actor);
        event.setTimestamp(Instant.now());
        event.setPayload(payload);
        auditClient.publishEvent(event);
    }

    private void applyStatusTransition(Case existing, CaseStatus nextStatus) {
        if (nextStatus == null || nextStatus == existing.getStatus()) {
            return;
        }
        if (existing.getStatus() == CaseStatus.CLOSED) {
            throw new BadRequestException("Closed cases cannot be reopened");
        }
        existing.setStatus(nextStatus);
    }
}
