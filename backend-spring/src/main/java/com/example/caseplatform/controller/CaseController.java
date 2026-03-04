package com.example.caseplatform.controller;

import com.example.caseplatform.domain.enums.CaseStatus;
import com.example.caseplatform.dto.CaseCreateRequest;
import com.example.caseplatform.dto.CaseResponse;
import com.example.caseplatform.dto.CaseUpdateRequest;
import com.example.caseplatform.service.CaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/cases")
public class CaseController {

    private final CaseService caseService;

    public CaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public Page<CaseResponse> getCases(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size,
            @RequestParam(required = false) CaseStatus status,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return caseService.getCases(status, search, pageable);
    }

    @GetMapping("/{id}")
    public CaseResponse getCase(@PathVariable UUID id) {
        return caseService.getCase(id);
    }

    @PostMapping
    public ResponseEntity<CaseResponse> createCase(
            @Valid @RequestBody CaseCreateRequest request,
            @RequestHeader(name = "X-Actor", defaultValue = "system") String actor) {
        CaseResponse created = caseService.createCase(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public CaseResponse updateCase(
            @PathVariable UUID id,
            @Valid @RequestBody CaseUpdateRequest request,
            @RequestHeader(name = "X-Actor", defaultValue = "system") String actor) {
        return caseService.updateCase(id, request, actor);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCase(
            @PathVariable UUID id,
            @RequestHeader(name = "X-Actor", defaultValue = "system") String actor) {
        caseService.deleteCase(id, actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/audit")
    public List<Map<String, Object>> getCaseAudit(@PathVariable UUID id) {
        return caseService.getCaseAudit(id);
    }
}
