package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class BatchController {

    public static final String METHOD = "method";
    private final MovieService service;

    public BatchController(MovieService service) {
        this.service = service;
    }

    // Write patterns
    @PostMapping("/batch/write/naive")
    public Map<String, Object> batchWriteNaive(@RequestParam(defaultValue = "10") int count) {
        int result = service.batchWriteBad(count);
        return Map.of("items", result, METHOD, "individual putItem calls");
    }

    @PostMapping("/batch/write/good")
    public Map<String, Object> batchWriteGood(@RequestParam(defaultValue = "10") int count) {
        int result = service.batchWriteGood(count);
        return Map.of("items", result, METHOD, "BatchWriteItem");
    }

    // Read patterns
    @GetMapping("/batch/read/naive")
    public Map<String, Object> batchReadNaive(@RequestParam List<String> ids) {
        int result = service.batchReadBad(ids);
        return Map.of("ids", ids, "requests", result, METHOD, "individual getItem calls");
    }

    @GetMapping("/batch/read/good")
    public Map<String, Object> batchReadGood(@RequestParam List<String> ids) {
        int result = service.batchReadGood(ids);
        return Map.of("ids", ids, "requests", result, METHOD, "BatchGetItem");
    }
}
