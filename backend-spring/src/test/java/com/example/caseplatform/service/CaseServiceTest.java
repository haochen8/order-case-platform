package com.example.caseplatform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AuditClient auditClient;

    private CaseService caseService;

    @BeforeEach
    void setUp() {
        caseService = new CaseService(caseRepository, orderRepository, new CaseMapper(), auditClient);
    }

    @Test
    void createCase_ShouldPersistAndPublishAuditEvent() {
        CaseCreateRequest request = new CaseCreateRequest();
        request.setTitle("Install fiber");
        request.setDescription("Customer wants a line upgrade");

        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> {
            Case entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            entity.setCreatedAt(Instant.parse("2026-03-04T10:00:00Z"));
            entity.setUpdatedAt(Instant.parse("2026-03-04T10:00:00Z"));
            return entity;
        });

        CaseResponse created = caseService.createCase(request, "tester");

        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(CaseStatus.OPEN);
        assertThat(created.getTitle()).isEqualTo("Install fiber");

        ArgumentCaptor<AuditEventRequest> eventCaptor = ArgumentCaptor.forClass(AuditEventRequest.class);
        verify(auditClient).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("CASE_CREATED");
        assertThat(eventCaptor.getValue().getEntityType()).isEqualTo("CASE");
    }

    @Test
    void updateCase_ShouldApplyChangesAndPublishEvent() {
        UUID caseId = UUID.randomUUID();

        Case existing = new Case();
        existing.setId(caseId);
        existing.setTitle("Old title");
        existing.setDescription("Old description");
        existing.setStatus(CaseStatus.OPEN);
        existing.setCreatedAt(Instant.parse("2026-03-01T08:00:00Z"));
        existing.setUpdatedAt(Instant.parse("2026-03-01T08:00:00Z"));

        CaseUpdateRequest request = new CaseUpdateRequest();
        request.setTitle("New title");
        request.setStatus(CaseStatus.CLOSED);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existing));
        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseResponse updated = caseService.updateCase(caseId, request, "tester");

        assertThat(updated.getTitle()).isEqualTo("New title");
        assertThat(updated.getStatus()).isEqualTo(CaseStatus.CLOSED);
        verify(auditClient).publishEvent(any(AuditEventRequest.class));
    }

    @Test
    void getCase_ShouldThrowWhenMissing() {
        UUID missingId = UUID.randomUUID();
        when(caseRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> caseService.getCase(missingId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Case not found");
    }

    @Test
    void updateCase_ShouldRejectReopeningClosedCase() {
        UUID caseId = UUID.randomUUID();
        Case existing = new Case();
        existing.setId(caseId);
        existing.setStatus(CaseStatus.CLOSED);

        CaseUpdateRequest request = new CaseUpdateRequest();
        request.setStatus(CaseStatus.OPEN);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> caseService.updateCase(caseId, request, "tester"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be reopened");
        verify(caseRepository, never()).save(any(Case.class));
    }
}
