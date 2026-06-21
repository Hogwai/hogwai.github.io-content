package com.hogwai.decider;

import com.hogwai.entity.ChainStep;
import com.hogwai.entity.Step;
import com.hogwai.repository.ChainStepRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.job.flow.FlowExecutionStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChainStepDeciderTest {

    private static final String CONFIG_NAME = "standard-order";
    private static final String STEP_NAME = "validateOrder";
    private static final String NEXT_ON_SUCCESS = "checkInventory";
    private static final String NEXT_ON_FAILURE = "escalateOrder";

    @Mock
    private ChainStepRepository chainStepRepository;

    private ChainStepDecider decider;

    @BeforeEach
    void setUp() {
        decider = new ChainStepDecider(chainStepRepository);
    }

    private ChainStep createChainStep(String successRoute, String failureRoute) {
        Step step = Step.builder().stepName(STEP_NAME).build();
        return ChainStep.builder()
            .currentStep(step)
            .nextStepOnSuccess(successRoute)
            .nextStepOnFailure(failureRoute)
            .build();
    }

    private JobExecution createJobExecution() {
        JobInstance jobInstance = new JobInstance(1L, "configurableOrderProcessingChain");
        JobParameters params = new JobParametersBuilder()
            .addString("chainConfigName", CONFIG_NAME)
            .toJobParameters();
        return new JobExecution(jobInstance, 1L, params);
    }

    @Test
    @DisplayName("Should route to nextStepOnSuccess when step completes successfully")
    void testSuccessStep() {
        JobExecution jobExecution = createJobExecution();
        StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution, 1L);
        stepExecution.setStatus(BatchStatus.COMPLETED);

        ChainStep chainStep = createChainStep(NEXT_ON_SUCCESS, NEXT_ON_FAILURE);
        when(chainStepRepository.findByStepAndConfiguration(STEP_NAME, CONFIG_NAME))
            .thenReturn(Optional.of(chainStep));

        FlowExecutionStatus result = decider.decide(jobExecution, stepExecution);

        assertEquals(NEXT_ON_SUCCESS, result.getName());
        verify(chainStepRepository).findByStepAndConfiguration(STEP_NAME, CONFIG_NAME);
    }

    @Test
    @DisplayName("Should route to nextStepOnFailure when step fails")
    void testFailureStep() {
        JobExecution jobExecution = createJobExecution();
        StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution, 1L);
        stepExecution.setStatus(BatchStatus.FAILED);

        ChainStep chainStep = createChainStep(NEXT_ON_SUCCESS, NEXT_ON_FAILURE);
        when(chainStepRepository.findByStepAndConfiguration(STEP_NAME, CONFIG_NAME))
            .thenReturn(Optional.of(chainStep));

        FlowExecutionStatus result = decider.decide(jobExecution, stepExecution);

        assertEquals(NEXT_ON_FAILURE, result.getName());
    }

    @Test
    @DisplayName("Should return FAILED when step is not found in configuration")
    void testNullStep() {
        JobExecution jobExecution = createJobExecution();
        StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution, 1L);
        stepExecution.setStatus(BatchStatus.COMPLETED);

        when(chainStepRepository.findByStepAndConfiguration(STEP_NAME, CONFIG_NAME))
            .thenReturn(Optional.empty());

        FlowExecutionStatus result = decider.decide(jobExecution, stepExecution);

        assertEquals(FlowExecutionStatus.FAILED.getName(), result.getName());
    }

    @Test
    @DisplayName("Should return first step name when stepExecution is null")
    void testNullStepExecution() {
        JobExecution jobExecution = createJobExecution();
        ChainStep firstStep = createChainStep(NEXT_ON_SUCCESS, NEXT_ON_FAILURE);
        when(chainStepRepository.findFirstStepByConfigName(eq(CONFIG_NAME),
            any(org.springframework.data.domain.Pageable.class)))
            .thenReturn(List.of(firstStep));

        FlowExecutionStatus result = decider.decide(jobExecution, null);

        assertEquals("validateOrder", result.getName());
    }
}
