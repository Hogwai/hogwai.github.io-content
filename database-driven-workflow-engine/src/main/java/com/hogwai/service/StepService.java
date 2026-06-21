package com.hogwai.service;

import com.hogwai.dto.StepRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.hogwai.repository.StepRepository;

@Service
public class StepService {

    private final StepRepository stepRepository;

    public StepService(StepRepository stepRepository) {
        this.stepRepository = stepRepository;
    }

    @Transactional(readOnly = true)
    public List<StepRecord> getAllStepsForChainConfigurations() {
        return stepRepository.findAllStepsForChainConfigurations();
    }
}
