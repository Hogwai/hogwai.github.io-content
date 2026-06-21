package com.hogwai.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.stereotype.Component;

@Component
public class ProcessCompletionListener implements JobExecutionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessCompletionListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        LOGGER.info("Starting job '{}' (execution {})",
            jobExecution.getJobInstance().getJobName(), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String processId = jobExecution.getJobParameters().getString("idProcess");
        String configName = jobExecution.getJobParameters().getString("chainConfigName");

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            LOGGER.warn("Job '{}' FAILED. Config: {}, Process: {}",
                jobExecution.getJobInstance().getJobName(), configName, processId);
        } else {
            LOGGER.info("Job '{}' COMPLETED successfully. Config: {}, Process: {}",
                jobExecution.getJobInstance().getJobName(), configName, processId);
        }

        LOGGER.info("Job execution finished: status={}, exitCode={}",
            jobExecution.getStatus(), jobExecution.getExitStatus().getExitCode());
    }
}
