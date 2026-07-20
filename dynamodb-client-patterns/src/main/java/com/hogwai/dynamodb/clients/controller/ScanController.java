package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScanController {

    private final MovieService service;

    public ScanController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies/scan")
    public Map<String, Object> scanMovies() {
        List<Movie> movies = service.scanBad();
        return Map.of("movies", movies, "count", movies.size(), "method", "full table scan");
    }

    @GetMapping("/movies/query")
    public Map<String, Object> queryByGenre(@RequestParam String genre) {
        List<Movie> movies = service.queryGood(genre);
        return Map.of("genre", genre, "movies", movies, "count", movies.size(), "method", "partition query");
    }
}
