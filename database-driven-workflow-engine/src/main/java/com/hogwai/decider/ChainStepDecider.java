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
        FlowExecutionStatus executionStatus = FlowExecutionStatus.UNKNOWN;
        if (stepExecution != null) {
            String stepName = stepExecution.getStepName();
            String config = jobExecution.getJobParameters().getString("chainConfigName");
            ChainStep currentStep =
                chainStepRepository.findByStepAndConfiguration(stepName, config).orElse(null);

            if (currentStep == null) {
                LOGGER.error("Step {} with configuration {} not found", stepName, config);
                return FlowExecutionStatus.FAILED;
            }

            if (stepExecution.getStatus() == BatchStatus.COMPLETED) {
                executionStatus = new FlowExecutionStatus(currentStep.getNextStepOnSuccess());
                LOGGER.info("Step {} completed. Next step: {}",
                    stepName, currentStep.getNextStepOnSuccess());
            } else {
                executionStatus = new FlowExecutionStatus(currentStep.getNextStepOnFailure());
                LOGGER.warn("Step {} ended in failure. Next step: {}",
                    stepName, currentStep.getNextStepOnFailure());
            }
        }
        return executionStatus;
    }
}
