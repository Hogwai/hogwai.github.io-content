package com.hogwai.tasklet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@StepScope
public class ChainInformationTasklet implements Tasklet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainInformationTasklet.class);

    private final String chainConfiguration;

    public ChainInformationTasklet(@Value("#{jobParameters[chainConfigName]}") String chainConfiguration) {
        this.chainConfiguration = chainConfiguration;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LOGGER.info("========================================");
        LOGGER.info("Launching chain configuration: {}", chainConfiguration);
        LOGGER.info("Job parameters: {}", chunkContext.getStepContext().getJobParameters());
        LOGGER.info("========================================");
        return RepeatStatus.FINISHED;
    }
}
