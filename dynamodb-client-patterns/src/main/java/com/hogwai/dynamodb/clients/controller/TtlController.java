package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TtlController {

    private final MovieService service;

    public TtlController(MovieService service) {
        this.service = service;
    }

    @PostMapping("/ttl/write/no-ttl")
    public Map<String, Object> ttlBad(@RequestParam(defaultValue = "5") int count) {
        int written = service.ttlBad(count);
        return Map.of("method", "write without TTL", "items", written,
                "note", "items accumulate forever, manual cleanup required");
    }

    @PostMapping("/ttl/write")
    public Map<String, Object> ttlGood(@RequestParam(defaultValue = "5") int count,
                                        @RequestParam(defaultValue = "3600") int ttlSeconds) {
        int written = service.ttlGood(count, ttlSeconds);
        return Map.of("method", "write with TTL (expireAt)", "items", written,
                "ttlSeconds", ttlSeconds,
                "note", "DynamoDB auto-deletes after TTL expiry");
    }
}
