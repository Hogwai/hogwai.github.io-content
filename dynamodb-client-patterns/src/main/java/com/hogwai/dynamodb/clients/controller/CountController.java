package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CountController {

    private final MovieService service;

    public CountController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/count")
    public Map<String, Object> count(@RequestParam String genre,
                                      @RequestParam(defaultValue = "false") boolean naive) {
        if (naive) {
            int count = service.countBad(genre);
            return Map.of("genre", genre, "count", count, "method", "naive");
        }
        int count = service.countGood(genre);
        return Map.of("genre", genre, "count", count, "method", "select_count");
    }
}
