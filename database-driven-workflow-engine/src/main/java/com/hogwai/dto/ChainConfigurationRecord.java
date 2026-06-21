package com.hogwai.dto;

import java.io.Serializable;
import java.util.List;

public record ChainConfigurationRecord(
    String chainName,
    String chainConfName,
    String chainConfDescription,
    List<ChainStepRecord> chainStepRecords
) implements Serializable {}
