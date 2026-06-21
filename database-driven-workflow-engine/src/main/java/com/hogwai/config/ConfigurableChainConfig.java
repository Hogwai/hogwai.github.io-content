package com.hogwai.config;

import static com.hogwai.enums.StepEnum.*;

import com.hogwai.decider.ChainStepDecider;
import com.hogwai.listener.ProcessCompletionListener;
import com.hogwai.tasklet.OrderProcessingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ConfigurableChainConfig {

    private final ChainStepDecider chainStepDecider;
    private final ProcessCompletionListener processCompletionListener;

    public ConfigurableChainConfig(ChainStepDecider chainStepDecider,
                                   ProcessCompletionListener processCompletionListener) {
        this.chainStepDecider = chainStepDecider;
        this.processCompletionListener = processCompletionListener;
    }

    @Bean
    public Job configurableOrderProcessingChain(JobRepository jobRepository,
                                                Step validateOrderStep,
                                                Step checkInventoryStep,
                                                Step processPaymentStep,
                                                Step applyDiscountStep,
                                                Step calculateTaxStep,
                                                Step fulfillOrderStep,
                                                Step sendConfirmationStep,
                                                Step updateAccountingStep,
                                                Step escalateOrderStep,
                                                Step archiveOrderStep,
                                                Step chainInformationStep) {
        return new JobBuilder("configurableOrderProcessingChain", jobRepository)
            .start(chainInformationStep)
            .next(chainStepDecider).on(VALIDATE_ORDER_STEP.getPattern()).to(validateOrderStep)
            .next(chainStepDecider).on(CHECK_INVENTORY_STEP.getPattern()).to(checkInventoryStep)
            .next(chainStepDecider).on(PROCESS_PAYMENT_STEP.getPattern()).to(processPaymentStep)
            .next(chainStepDecider).on(APPLY_DISCOUNT_STEP.getPattern()).to(applyDiscountStep)
            .next(chainStepDecider).on(CALCULATE_TAX_STEP.getPattern()).to(calculateTaxStep)
            .next(chainStepDecider).on(FULFILL_ORDER_STEP.getPattern()).to(fulfillOrderStep)
            .next(chainStepDecider).on(SEND_CONFIRMATION_STEP.getPattern()).to(sendConfirmationStep)
            .next(chainStepDecider).on(UPDATE_ACCOUNTING_STEP.getPattern()).to(updateAccountingStep)
            .next(chainStepDecider).on(ESCALATE_ORDER_STEP.getPattern()).to(escalateOrderStep)
            .next(chainStepDecider).on(ARCHIVE_ORDER_STEP.getPattern()).to(archiveOrderStep)
            .end()
            .listener(processCompletionListener)
            .build();
    }

    @Bean
    public Step chainInformationStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     Tasklet chainInformationTasklet) {
        return new StepBuilder("chainInformationStep", jobRepository)
            .tasklet(chainInformationTasklet, transactionManager)
            .build();
    }

    @Bean
    public Step validateOrderStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  OrderProcessingTasklet tasklet) {
        return new StepBuilder(VALIDATE_ORDER_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step checkInventoryStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   OrderProcessingTasklet tasklet) {
        return new StepBuilder(CHECK_INVENTORY_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step processPaymentStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   OrderProcessingTasklet tasklet) {
        return new StepBuilder(PROCESS_PAYMENT_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step applyDiscountStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  OrderProcessingTasklet tasklet) {
        return new StepBuilder(APPLY_DISCOUNT_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step calculateTaxStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 OrderProcessingTasklet tasklet) {
        return new StepBuilder(CALCULATE_TAX_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step fulfillOrderStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 OrderProcessingTasklet tasklet) {
        return new StepBuilder(FULFILL_ORDER_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step sendConfirmationStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     OrderProcessingTasklet tasklet) {
        return new StepBuilder(SEND_CONFIRMATION_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step updateAccountingStep(JobRepository jobRepository,
                                     PlatformTransactionManager transactionManager,
                                     OrderProcessingTasklet tasklet) {
        return new StepBuilder(UPDATE_ACCOUNTING_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step escalateOrderStep(JobRepository jobRepository,
                                  PlatformTransactionManager transactionManager,
                                  OrderProcessingTasklet tasklet) {
        return new StepBuilder(ESCALATE_ORDER_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }

    @Bean
    public Step archiveOrderStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 OrderProcessingTasklet tasklet) {
        return new StepBuilder(ARCHIVE_ORDER_STEP.getPattern(), jobRepository)
            .tasklet(tasklet, transactionManager).build();
    }
}
