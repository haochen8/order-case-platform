package com.example.caseplatform.service;

import com.example.caseplatform.dto.AuditEventRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AuditClient {

    private static final Logger log = LoggerFactory.getLogger(AuditClient.class);

    private final WebClient auditWebClient;

    public AuditClient(WebClient auditWebClient) {
        this.auditWebClient = auditWebClient;
    }

    public void publishEvent(AuditEventRequest event) {
        try {
            auditWebClient.post()
                    .uri("/audit-events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(event)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception ex) {
            log.warn("Failed to publish audit event {} for {}", event.getEventType(), event.getEntityId(), ex);
        }
    }

    public List<Map<String, Object>> getEvents(String entityType, UUID entityId) {
        try {
            return auditWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/audit-events")
                            .queryParam("entityType", entityType)
                            .queryParam("entityId", entityId)
                            .build())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {
                    })
                    .blockOptional()
                    .orElse(Collections.emptyList());
        } catch (Exception ex) {
            log.warn("Failed to fetch audit history for {} {}", entityType, entityId, ex);
            return Collections.emptyList();
        }
    }
}
