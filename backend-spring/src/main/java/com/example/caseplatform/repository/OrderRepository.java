package com.example.caseplatform.repository;

import com.example.caseplatform.domain.Order;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findByCaseId(UUID caseId, Pageable pageable);

    void deleteByCaseId(UUID caseId);
}
