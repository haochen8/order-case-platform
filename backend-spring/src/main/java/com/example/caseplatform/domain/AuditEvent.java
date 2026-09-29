package com.example.caseplatform.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, updatable = false)
    private UUID caseId;
    @Column(nullable = false, updatable = false, length = 32)
    private String entityType;
    @Column(nullable = false, updatable = false)
    private UUID entityId;
    @Column(nullable = false, updatable = false, length = 64)
    private String eventType;
    @Column(nullable = false, updatable = false)
    private String actor;
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, updatable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    protected AuditEvent() {}

    public AuditEvent(UUID caseId, String entityType, UUID entityId, String eventType,
                      String actor, Map<String, Object> payload) {
        this.caseId = caseId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.eventType = eventType;
        this.actor = actor;
        this.occurredAt = Instant.now();
        this.payload = Map.copyOf(payload);
    }

    public UUID getId() { return id; }
    public UUID getCaseId() { return caseId; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public String getEventType() { return eventType; }
    public String getActor() { return actor; }
    public Instant getOccurredAt() { return occurredAt; }
    public Map<String, Object> getPayload() { return payload; }
}
