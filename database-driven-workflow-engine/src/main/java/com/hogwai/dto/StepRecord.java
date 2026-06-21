package com.hogwai.dto;

import java.io.Serializable;

public record StepRecord(
    String stepName,
    String stepDescription
) implements Serializable {}
