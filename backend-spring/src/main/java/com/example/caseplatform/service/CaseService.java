package com.example.caseplatform.service;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.domain.enums.CaseStatus;
import com.example.caseplatform.domain.enums.OrderStatus;
import com.example.caseplatform.dto.CaseCreateRequest;
import com.example.caseplatform.dto.CaseResponse;
import com.example.caseplatform.dto.CaseUpdateRequest;
import com.example.caseplatform.exception.BadRequestException;
import com.example.caseplatform.exception.ConflictException;
import com.example.caseplatform.exception.ResourceNotFoundException;
import com.example.caseplatform.mapper.CaseMapper;
import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.repository.OrderRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private final AuditService audit;

    public CaseService(CaseRepository caseRepository, OrderRepository orderRepository,
                       CaseMapper caseMapper, AuditService audit) {
        this.caseRepository = caseRepository;
        this.orderRepository = orderRepository;
        this.caseMapper = caseMapper;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<CaseResponse> getCases(CaseStatus status, String search, Pageable pageable) {
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;
        return caseRepository.findByFilters(status, normalizedSearch, pageable).map(caseMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCase(UUID id) { return caseMapper.toResponse(findCaseById(id)); }

    @Transactional
    public CaseResponse createCase(CaseCreateRequest request, String actor) {
        Case entity = new Case();
        entity.setTitle(request.getTitle().trim());
        entity.setDescription(request.getDescription());
        entity.setStatus(CaseStatus.OPEN);
        Case created = caseRepository.saveAndFlush(entity);
        audit.record(created.getId(), "CASE", created.getId(), "CASE_CREATED", actor,
                Map.of("after", snapshot(created)));
        return caseMapper.toResponse(created);
    }

    @Transactional
    public CaseResponse updateCase(UUID id, CaseUpdateRequest request, String actor) {
        Case existing = caseRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
        requireVersion(existing, request.getVersion());
        Map<String, Object> before = snapshot(existing);
        if (request.getTitle() != null) existing.setTitle(request.getTitle().trim());
        if (request.getDescription() != null) existing.setDescription(request.getDescription());
        applyStatusTransition(existing, request.getStatus());
        Case updated = caseRepository.saveAndFlush(existing);
        Map<String, Object> after = snapshot(updated);
        if (!before.equals(after)) {
            audit.record(id, "CASE", id, "CASE_UPDATED", actor, Map.of("before", before, "after", after));
        }
        return caseMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCase(UUID id, Long version, String actor) {
        Case existing = caseRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
        requireVersion(existing, version);
        if (orderRepository.existsByCaseId(id)) {
            throw new ConflictException("Cases with orders cannot be deleted; close the case instead");
        }
        audit.record(id, "CASE", id, "CASE_DELETED", actor, Map.of("before", snapshot(existing)));
        caseRepository.delete(existing);
        caseRepository.flush();
    }

    private Case findCaseById(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
    }

    private void requireVersion(Case entity, Long expected) {
        if (expected == null || !Objects.equals(entity.getVersion(), expected)) {
            throw new ConflictException("Case changed; reload it before retrying");
        }
    }

    private Map<String, Object> snapshot(Case entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("title", entity.getTitle());
        result.put("description", entity.getDescription());
        result.put("status", entity.getStatus().name());
        return result;
    }

    private void applyStatusTransition(Case existing, CaseStatus nextStatus) {
        if (nextStatus == null || nextStatus == existing.getStatus()) return;
        if (existing.getStatus() == CaseStatus.CLOSED) {
            throw new BadRequestException("Closed cases cannot be reopened");
        }
        if (existing.getStatus() == CaseStatus.IN_PROGRESS && nextStatus == CaseStatus.OPEN) {
            throw new BadRequestException("In-progress cases cannot return to open");
        }
        if (nextStatus == CaseStatus.CLOSED && orderRepository.existsByCaseIdAndStatusIn(
                existing.getId(), List.of(OrderStatus.PENDING, OrderStatus.SENT))) {
            throw new ConflictException("Complete or fail outstanding orders before closing the case");
        }
        existing.setStatus(nextStatus);
    }
}
