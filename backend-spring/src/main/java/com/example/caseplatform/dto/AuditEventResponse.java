package com.example.caseplatform.dto;

import com.example.caseplatform.domain.AuditEvent;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(UUID id, UUID caseId, String entityType, UUID entityId,
                                 String eventType, String actor, Instant timestamp,
                                 Map<String, Object> payload) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(event.getId(), event.getCaseId(), event.getEntityType(),
                event.getEntityId(), event.getEventType(), event.getActor(),
                event.getOccurredAt(), event.getPayload());
    }
}
