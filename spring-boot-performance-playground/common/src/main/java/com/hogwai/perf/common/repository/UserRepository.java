package com.hogwai.perf.common.repository;

import com.hogwai.perf.common.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT COUNT(u) FROM User u")
    long countUsers();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o")
    double totalOrderValue();
}
