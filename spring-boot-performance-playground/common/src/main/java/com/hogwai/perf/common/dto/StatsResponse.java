package com.hogwai.perf.common.dto;

import java.io.Serializable;

public record StatsResponse(long totalUsers, long totalOrders, long totalProducts, double averageOrderValue) implements Serializable {}
