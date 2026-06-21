package com.hogwai.service.impl;

import com.hogwai.dto.ChainConfigurationRecord;
import com.hogwai.dto.ChainStepRecord;
import com.hogwai.entity.Chain;
import com.hogwai.entity.ChainConfiguration;
import com.hogwai.entity.ChainStep;
import com.hogwai.entity.Step;
import com.hogwai.enums.ChainStatusEnum;
import com.hogwai.mapper.ChainConfigurationMapper;
import com.hogwai.repository.*;
import com.hogwai.service.ChainConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ChainConfigurationServiceImpl implements ChainConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainConfigurationServiceImpl.class);
    private static final String STEP_NOT_FOUND_ERROR = "Step with name %s not found";

    private final ChainConfigurationRepository chainConfigurationRepository;
    private final StepRepository stepRepository;
    private final ChainRepository chainRepository;
    private final ChainStatusRepository chainStatusRepository;
    private final ChainStepRepository chainStepRepository;

    public ChainConfigurationServiceImpl(ChainConfigurationRepository chainConfigurationRepository,
                                         StepRepository stepRepository,
                                         ChainRepository chainRepository,
                                         ChainStatusRepository chainStatusRepository,
                                         ChainStepRepository chainStepRepository) {
        this.chainConfigurationRepository = chainConfigurationRepository;
        this.stepRepository = stepRepository;
        this.chainRepository = chainRepository;
        this.chainStatusRepository = chainStatusRepository;
        this.chainStepRepository = chainStepRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public ChainConfigurationRecord getChainConfigurationByName(String chainConfName) {
        ChainConfiguration chainConfiguration = chainConfigurationRepository.findByConfName(chainConfName)
            .orElseThrow(() -> new IllegalArgumentException(
                "Chain configuration with name %s not found".formatted(chainConfName)));
        return ChainConfigurationMapper.INSTANCE.toChainConfigurationRecord(chainConfiguration);
    }

    @Override
    @Transactional
    public ChainConfigurationRecord createChainConfiguration(ChainConfigurationRecord configurationRecord) {
        if (chainConfigurationRepository.findByConfName(configurationRecord.chainConfName()).isPresent()) {
            throw new IllegalArgumentException("The chain configuration %s already exists"
                .formatted(configurationRecord.chainConfName()));
        }

        LOGGER.info("Creating configuration {} for chain {}...",
                configurationRecord.chainConfName(), configurationRecord.chainName());

        Chain chain = chainRepository.findByChainName(configurationRecord.chainName())
            .orElseThrow(() -> new IllegalArgumentException(
                "Chain with name %s not found".formatted(configurationRecord.chainName())));

        LocalDateTime now = LocalDateTime.now();
        ChainConfiguration newConfig = ChainConfiguration.builder()
            .chain(chain)
            .confName(configurationRecord.chainConfName())
            .description(configurationRecord.chainConfDescription())
            .chainStatus(chainStatusRepository.getReferenceById(ChainStatusEnum.ACTIVE.getId()))
            .creationDate(now)
            .build();

        List<ChainStep> chainSteps = generateChainStepsFromRecords(configurationRecord, newConfig, now);
        newConfig.setChainSteps(chainSteps);

        ChainConfiguration saved = chainConfigurationRepository.saveAndFlush(newConfig);
        LOGGER.info("Configuration {} saved with {} steps", saved.getConfName(), chainSteps.size());

        return ChainConfigurationMapper.INSTANCE.toChainConfigurationRecord(saved);
    }

    @Override
    @Transactional
    public ChainConfigurationRecord updateChainConfiguration(ChainConfigurationRecord configurationRecord) {
        if (chainRepository.findByChainName(configurationRecord.chainName()).isEmpty()) {
            throw new IllegalArgumentException("Chain with name %s not found"
                .formatted(configurationRecord.chainName()));
        }

        ChainConfiguration configToUpdate = chainConfigurationRepository.findByConfName(configurationRecord.chainConfName())
            .orElseThrow(() -> new IllegalArgumentException(
                "Chain configuration %s not found".formatted(configurationRecord.chainConfName())));

        chainStepRepository.deleteAllByChainConfiguration(configToUpdate);

        LocalDateTime now = LocalDateTime.now();
        List<ChainStep> chainSteps = generateChainStepsFromRecords(configurationRecord, configToUpdate, now);
        configToUpdate.setUpdateDate(now);
        configToUpdate.setChainSteps(chainSteps);

        ChainConfiguration updated = chainConfigurationRepository.save(configToUpdate);
        return ChainConfigurationMapper.INSTANCE.toChainConfigurationRecord(updated);
    }

    private List<ChainStep> generateChainStepsFromRecords(ChainConfigurationRecord configurationRecord,
                                                          ChainConfiguration config,
                                                          LocalDateTime now) {
        return new ArrayList<>(configurationRecord.chainStepRecords().stream().map(stepRecord -> {
            Step step = stepRepository.findByStepName(stepRecord.stepName())
                .orElseThrow(() -> new IllegalArgumentException(
                    STEP_NOT_FOUND_ERROR.formatted(stepRecord.stepName())));
            verifyStepsExistence(stepRecord);

            return ChainStep.builder()
                .currentStep(step)
                .chainConfiguration(config)
                .chainStatus(chainStatusRepository.getReferenceById(ChainStatusEnum.ACTIVE.getId()))
                .nextStepOnSuccess(stepRecord.nextStepOnSuccess())
                .nextStepOnFailure(stepRecord.nextStepOnFailure())
                .creationDate(now)
                .build();
        }).toList());
    }

    private void verifyStepsExistence(ChainStepRecord stepRecord) {
        if (stepRecord.nextStepOnSuccess() == null) {
            throw new IllegalArgumentException(
                "A next step on success must be provided for step %s".formatted(stepRecord.stepName()));
        }
        if (!stepRepository.existsByStepName(stepRecord.nextStepOnSuccess())) {
            throw new IllegalArgumentException(
                STEP_NOT_FOUND_ERROR.formatted(stepRecord.nextStepOnSuccess()));
        }
        if (stepRecord.nextStepOnFailure() != null
            && !stepRepository.existsByStepName(stepRecord.nextStepOnFailure())) {
            throw new IllegalArgumentException(
                STEP_NOT_FOUND_ERROR.formatted(stepRecord.nextStepOnFailure()));
        }
    }
}
