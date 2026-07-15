package com.hogwai.perf.common.dto;

import java.io.Serializable;

public record UserResponse(Long id, String name, String email, int loyaltyPoints, int orderCount) implements Serializable {}
