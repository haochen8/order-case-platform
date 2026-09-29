package com.example.caseplatform.service;

import com.example.caseplatform.domain.Case;
import com.example.caseplatform.domain.Order;
import com.example.caseplatform.domain.enums.CaseStatus;
import com.example.caseplatform.domain.enums.OrderStatus;
import com.example.caseplatform.dto.OrderCreateRequest;
import com.example.caseplatform.dto.OrderResponse;
import com.example.caseplatform.dto.OrderUpdateRequest;
import com.example.caseplatform.exception.BadRequestException;
import com.example.caseplatform.exception.ConflictException;
import com.example.caseplatform.exception.ResourceNotFoundException;
import com.example.caseplatform.repository.CaseRepository;
import com.example.caseplatform.repository.OrderRepository;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final CaseRepository caseRepository;
    private final AuditService audit;

    public OrderService(OrderRepository orderRepository, CaseRepository caseRepository, AuditService audit) {
        this.orderRepository = orderRepository;
        this.caseRepository = caseRepository;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(UUID caseId, Pageable pageable) {
        return (caseId == null ? orderRepository.findAll(pageable) : orderRepository.findByCaseId(caseId, pageable))
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) { return toResponse(findOrder(id)); }

    @Transactional
    public OrderResponse createOrder(OrderCreateRequest request, String actor) {
        Case parent = lockCase(request.getCaseId());
        if (parent.getStatus() == CaseStatus.CLOSED) {
            throw new ConflictException("Cannot add orders to a closed case");
        }
        if (request.getStatus() != null && request.getStatus() != OrderStatus.PENDING) {
            throw new BadRequestException("New orders must start as PENDING");
        }
        Order entity = new Order();
        entity.setCaseId(parent.getId());
        entity.setType(request.getType().trim());
        entity.setStatus(OrderStatus.PENDING);
        Order created = orderRepository.saveAndFlush(entity);
        audit.record(parent.getId(), "ORDER", created.getId(), "ORDER_CREATED", actor,
                Map.of("type", created.getType(), "status", created.getStatus().name()));
        return toResponse(created);
    }

    @Transactional
    public OrderResponse updateOrder(UUID id, OrderUpdateRequest request, String actor) {
        UUID caseId = orderRepository.findCaseId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        Case parent = lockCase(caseId);
        Order order = findOrder(id);
        if (parent.getStatus() == CaseStatus.CLOSED) throw new ConflictException("Case is closed");
        if (!Objects.equals(order.getVersion(), request.version())) {
            throw new ConflictException("Order changed; reload it before retrying");
        }
        OrderStatus before = order.getStatus();
        OrderStatus after = request.status();
        if (before == after) return toResponse(order);
        boolean allowed = (before == OrderStatus.PENDING && (after == OrderStatus.SENT || after == OrderStatus.FAILED))
                || (before == OrderStatus.SENT && (after == OrderStatus.COMPLETED || after == OrderStatus.FAILED));
        if (!allowed) throw new BadRequestException("Invalid order transition: " + before + " -> " + after);
        order.setStatus(after);
        Order updated = orderRepository.saveAndFlush(order);
        audit.record(parent.getId(), "ORDER", id, "ORDER_UPDATED", actor,
                Map.of("previousStatus", before.name(), "status", after.name()));
        return toResponse(updated);
    }

    private Case lockCase(UUID id) {
        return caseRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + id));
    }

    private Order findOrder(UUID id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setCaseId(order.getCaseId());
        response.setType(order.getType());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setVersion(order.getVersion());
        return response;
    }
}
