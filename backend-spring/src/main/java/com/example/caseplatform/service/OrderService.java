package com.example.caseplatform.service;

import com.example.caseplatform.domain.Order;
import com.example.caseplatform.domain.enums.OrderStatus;
import com.example.caseplatform.dto.AuditEventRequest;
import com.example.caseplatform.dto.OrderCreateRequest;
import com.example.caseplatform.dto.OrderResponse;
import com.example.caseplatform.exception.ResourceNotFoundException;
import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.repository.OrderRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final CaseRepository caseRepository;
    private final AuditClient auditClient;

    public OrderService(
            OrderRepository orderRepository,
            CaseRepository caseRepository,
            AuditClient auditClient) {
        this.orderRepository = orderRepository;
        this.caseRepository = caseRepository;
        this.auditClient = auditClient;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(UUID caseId, Pageable pageable) {
        if (caseId != null) {
            return orderRepository.findByCaseId(caseId, pageable).map(this::toResponse);
        }
        return orderRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String actor) {
        if (!caseRepository.existsById(request.getCaseId())) {
            throw new ResourceNotFoundException("Case not found: " + request.getCaseId());
        }

        Order toCreate = new Order();
        toCreate.setCaseId(request.getCaseId());
        toCreate.setType(request.getType().trim());
        toCreate.setStatus(request.getStatus() != null ? request.getStatus() : OrderStatus.PENDING);

        Order created = orderRepository.save(toCreate);
        publishOrderEvent(
                "ORDER_CREATED",
                created.getId(),
                actor,
                Map.of(
                        "caseId", created.getCaseId().toString(),
                        "status", created.getStatus().name(),
                        "type", created.getType()));

        return toResponse(created);
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCaseId(order.getCaseId());
        response.setType(order.getType());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        return response;
    }

    private void publishOrderEvent(String eventType, UUID orderId, String actor, Map<String, Object> payload) {
        AuditEventRequest event = new AuditEventRequest();
        event.setEventType(eventType);
        event.setEntityType("ORDER");
        event.setEntityId(orderId.toString());
        event.setActor(actor);
        event.setTimestamp(Instant.now());
        event.setPayload(payload);
        auditClient.publishEvent(event);
    }
}
