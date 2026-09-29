package com.example.caseplatform.repository;

import com.example.caseplatform.domain.Order;
import com.example.caseplatform.domain.enums.OrderStatus;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    @Query("SELECT o.caseId FROM Order o WHERE o.id = :id")
    Optional<UUID> findCaseId(@Param("id") UUID id);

    Page<Order> findByCaseId(UUID caseId, Pageable pageable);

    boolean existsByCaseId(UUID caseId);

    boolean existsByCaseIdAndStatusIn(UUID caseId, Collection<OrderStatus> statuses);
}
