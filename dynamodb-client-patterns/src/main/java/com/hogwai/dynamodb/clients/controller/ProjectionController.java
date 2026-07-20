package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ProjectionController {

    private final MovieService service;

    public ProjectionController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies")
    public Map<String, Object> getMovies(
            @RequestParam String genre,
            @RequestParam(required = false) List<String> fields) {
        if (fields == null || fields.isEmpty()) {
            List<Movie> movies = service.projectionBad(genre);
            return Map.of("genre", genre, "movies", movies, "method", "full item");
        }
        List<Movie> movies = service.projectionGood(genre, fields);
        return Map.of("genre", genre, "movies", movies, "fields", fields, "method", "projected");
    }
}
