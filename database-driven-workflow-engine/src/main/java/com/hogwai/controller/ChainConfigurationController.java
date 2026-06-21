package com.hogwai.controller;

import com.hogwai.dto.ChainConfigurationRecord;
import com.hogwai.dto.StepRecord;
import com.hogwai.service.ChainConfigurationService;
import com.hogwai.service.StepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/chain-config")
@Tag(name = "Chain Configuration API", description = "Manage and invoke configurable workflow pipelines")
public class ChainConfigurationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChainConfigurationController.class);

    private final ChainConfigurationService chainConfigurationService;
    private final Job configurableOrderProcessingChain;
    private final JobLauncher jobLauncher;
    private final StepService stepService;

    public ChainConfigurationController(ChainConfigurationService chainConfigurationService,
                                        Job configurableOrderProcessingChain,
                                        JobLauncher jobLauncher,
                                        StepService stepService) {
        this.chainConfigurationService = chainConfigurationService;
        this.configurableOrderProcessingChain = configurableOrderProcessingChain;
        this.jobLauncher = jobLauncher;
        this.stepService = stepService;
    }

    @GetMapping("/steps")
    @Operation(summary = "Get all available steps",
        description = "List all reusable processing steps that can be used in chain configurations")
    public ResponseEntity<List<StepRecord>> getAllSteps() {
        return ResponseEntity.ok(stepService.getAllStepsForChainConfigurations());
    }

    @GetMapping(value = "/{chainConfName}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get a chain configuration by name",
        description = "Retrieve a chain configuration and its step routing table")
    public ResponseEntity<ChainConfigurationRecord> getByName(@PathVariable String chainConfName) {
        return ResponseEntity.ok(chainConfigurationService.getChainConfigurationByName(chainConfName));
    }

    @PostMapping(value = "/create", produces = MediaType.APPLICATION_JSON_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a new chain configuration",
        description = "Create a new pipeline configuration with step routing rules")
    public ResponseEntity<ChainConfigurationRecord> create(
            @RequestBody ChainConfigurationRecord configurationRecord) {
        return ResponseEntity.ok(chainConfigurationService.createChainConfiguration(configurationRecord));
    }

    @PutMapping("/update")
    @Operation(summary = "Update a chain configuration",
        description = "Update an existing pipeline configuration's step routing")
    public String update(@RequestBody ChainConfigurationRecord configurationRecord) {
        ChainConfigurationRecord updated = chainConfigurationService.updateChainConfiguration(configurationRecord);
        return "Configuration %s updated successfully".formatted(updated.chainConfName());
    }

    @GetMapping("/invoke")
    @Operation(summary = "Invoke a chain configuration",
        description = "Launch a workflow pipeline for the given configuration and order ID")
    public String invoke(@RequestParam("config") String config,
                         @RequestParam("orderId") String orderId)
        throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException,
        JobParametersInvalidException, JobRestartException {

        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("time", System.currentTimeMillis())
            .addString("chainConfigName", config)
            .addString("idProcess", orderId)
            .toJobParameters();

        jobLauncher.run(configurableOrderProcessingChain, jobParameters);
        LOGGER.info("Invoked chain config: {} for order: {}", config, orderId);
        return "Pipeline '%s' launched for order %s".formatted(config, orderId);
    }
}
