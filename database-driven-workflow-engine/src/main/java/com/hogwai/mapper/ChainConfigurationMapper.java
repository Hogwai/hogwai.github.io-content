package com.hogwai.mapper;

import com.hogwai.dto.ChainConfigurationRecord;
import com.hogwai.dto.ChainStepRecord;
import com.hogwai.entity.ChainConfiguration;
import com.hogwai.entity.ChainStep;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper
public interface ChainConfigurationMapper {
    ChainConfigurationMapper INSTANCE = Mappers.getMapper(ChainConfigurationMapper.class);

    @Mapping(source = "chain.chainName", target = "chainName")
    @Mapping(source = "confName", target = "chainConfName")
    @Mapping(source = "description", target = "chainConfDescription")
    @Mapping(target = "chainStepRecords",
        expression = "java(toChainStepRecords(chainConfiguration.getChainSteps()))")
    ChainConfigurationRecord toChainConfigurationRecord(ChainConfiguration chainConfiguration);

    List<ChainStepRecord> toChainStepRecords(List<ChainStep> chainSteps);

    @Mapping(source = "currentStep.stepName", target = "stepName")
    @Mapping(source = "nextStepOnSuccess", target = "nextStepOnSuccess")
    @Mapping(source = "nextStepOnFailure", target = "nextStepOnFailure")
    ChainStepRecord toChainStepRecord(ChainStep chainStep);
}
