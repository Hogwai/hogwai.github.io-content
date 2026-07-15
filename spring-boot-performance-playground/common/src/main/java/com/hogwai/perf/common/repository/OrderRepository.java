package com.hogwai.perf.common.repository;

import com.hogwai.perf.common.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByUserId(Long userId);
}
