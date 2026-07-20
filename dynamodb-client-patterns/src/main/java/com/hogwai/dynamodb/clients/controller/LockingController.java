package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class LockingController {

    private final MovieService service;

    public LockingController(MovieService service) {
        this.service = service;
    }

    @PutMapping("/lock/bad")
    public Map<String, Object> lockingBad(@RequestParam String genre,
                                           @RequestParam String movieId,
                                           @RequestParam String title) {
        boolean updated = service.lockingBad(genre, movieId, title);
        return Map.of(
                "method", "unconditional update (no version check)",
                "updated", updated,
                "note", "Lost update risk — concurrent writes may overwrite each other");
    }

    @PutMapping("/lock/good")
    public Map<String, Object> lockingGood(@RequestParam String genre,
                                            @RequestParam String movieId,
                                            @RequestParam String title,
                                            @RequestParam int expectedVersion) {
        boolean updated = service.lockingGood(genre, movieId, title, expectedVersion);
        return Map.of(
                "method", "optimistic locking with version check",
                "updated", updated,
                "expectedVersion", expectedVersion,
                "newVersion", updated ? expectedVersion + 1 : expectedVersion);
    }
}
