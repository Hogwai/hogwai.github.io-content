package com.hogwai.perf.optimized.controller;

import com.hogwai.perf.common.dto.StatsResponse;
import com.hogwai.perf.common.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/stats")
    public StatsResponse getStats() {
        return statsService.getStats();
    }
}
