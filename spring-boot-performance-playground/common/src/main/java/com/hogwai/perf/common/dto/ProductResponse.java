package com.hogwai.perf.common.dto;

import java.io.Serializable;

public record ProductResponse(Long id, String name, String description, double price, int stockQuantity) implements Serializable {}
