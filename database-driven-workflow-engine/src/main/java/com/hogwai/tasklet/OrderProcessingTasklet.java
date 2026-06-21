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
public class OrderProcessingTasklet implements Tasklet {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderProcessingTasklet.class);

    private final String stepName;
    private final String description;

    public OrderProcessingTasklet(@Value("#{stepExecution.stepName}") String stepName,
                                  @Value("#{jobParameters['chainConfigName']}") String description) {
        this.stepName = stepName;
        this.description = description;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        LOGGER.info("─── {} ───", stepName);

        // Simulate processing based on step name
        switch (stepName) {
            case "validateOrder" -> {
                LOGGER.info("Validating order data...");
                simulateWork("items checked", "customer verified", "address confirmed");
            }
            case "checkInventory" -> {
                LOGGER.info("Checking stock availability...");
                simulateWork("stock verified", "warehouse located", "picklist generated");
            }
            case "processPayment" -> {
                LOGGER.info("Processing payment via gateway...");
                simulateWork("auth obtained", "capture initiated", "receipt generated");
            }
            case "applyDiscount" -> {
                LOGGER.info("Applying promotions and loyalty discounts...");
                simulateWork("loyalty tier checked", "promo codes applied", "discount calculated");
            }
            case "calculateTax" -> {
                LOGGER.info("Calculating sales tax...");
                simulateWork("tax jurisdiction resolved", "rate applied", "total computed");
            }
            case "fulfillOrder" -> {
                LOGGER.info("Fulfilling order...");
                simulateWork("items packed", "label printed", "carrier assigned");
            }
            case "sendConfirmation" -> {
                LOGGER.info("Sending order confirmation...");
                simulateWork("email queued", "SMS sent", "receipt attached");
            }
            case "updateAccounting" -> {
                LOGGER.info("Updating financial ledgers...");
                simulateWork("revenue recorded", "inventory adjusted", "tax liability logged");
            }
            case "escalateOrder" -> {
                LOGGER.info("Escalating order for manual review...");
                simulateWork("fraud alert checked", "review ticket created", "notification sent");
            }
            case "archiveOrder" -> {
                LOGGER.info("Archiving order records...");
                simulateWork("order snapshotted", "records compressed", "retention tag applied");
            }
            default -> LOGGER.warn("Unknown step: {}", stepName);
        }

        LOGGER.info("Step completed successfully (config: {})", description);
        return RepeatStatus.FINISHED;
    }

    private void simulateWork(String... tasks) {
        for (String task : tasks) {
            LOGGER.debug("  · {}", task);
            try {
                Thread.sleep(300);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
