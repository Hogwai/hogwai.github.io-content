package com.hogwai.decider;

import com.hogwai.entity.ChainStep;
import com.hogwai.repository.ChainStepRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.stereotype.Component;

@Component
public class ChainStepDecider implements JobExecutionDecider {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainStepDecider.class);

    private final ChainStepRepository chainStepRepository;

    public ChainStepDecider(ChainStepRepository chainStepRepository) {
        this.chainStepRepository = chainStepRepository;
    }

    @Override
    public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
        String config = jobExecution.getJobParameters().getString("chainConfigName");

        // Special case: chainInformationStep or null stepExecution
        if (stepExecution == null || "chainInformationStep".equals(stepExecution.getStepName())) {
            var steps = chainStepRepository.findFirstStepByConfigName(config,
                org.springframework.data.domain.PageRequest.of(0, 1));
            String firstStepName = steps.isEmpty() ? null : steps.getFirst().getCurrentStep().getStepName();
            if (firstStepName != null) {
                LOGGER.info("Starting chain '{}' with first step: {}", config, firstStepName);
                return new FlowExecutionStatus(firstStepName);
            }
            LOGGER.error("No steps found for configuration {}", config);
            return FlowExecutionStatus.FAILED;
        }

        // Normal flow: look up current step in DB
        String stepName = stepExecution.getStepName();
        ChainStep currentStep =
            chainStepRepository.findByStepAndConfiguration(stepName, config).orElse(null);

        if (currentStep == null) {
            LOGGER.error("Step {} with configuration {} not found", stepName, config);
            return FlowExecutionStatus.FAILED;
        }

        if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
            LOGGER.info("Step {} completed. Next step: {}",
                stepName, currentStep.getNextStepOnSuccess());
            return new FlowExecutionStatus(currentStep.getNextStepOnSuccess());
        } else {
            LOGGER.warn("Step {} ended in failure. Next step: {}",
                stepName, currentStep.getNextStepOnFailure());
            return new FlowExecutionStatus(currentStep.getNextStepOnFailure());
        }
    }
}
