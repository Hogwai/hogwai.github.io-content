package com.hogwai.perf.common.dto;

import java.io.Serializable;
import java.util.List;

public record DashboardResponse(UserResponse user, List<OrderSummary> recentOrders, int loyaltyPoints) implements Serializable {
    public record OrderSummary(Long id, String status, double totalAmount) implements Serializable {}
}
