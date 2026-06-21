package com.hogwai.dto;

import java.io.Serializable;

public record ChainStepRecord(
    String stepName,
    String nextStepOnSuccess,
    String nextStepOnFailure
) implements Serializable {}
