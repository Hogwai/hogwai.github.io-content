package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PaginationController {

    private final MovieService service;

    public PaginationController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies/paged")
    public Map<String, Object> getMoviesPaged(
            @RequestParam String genre,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "10") int size) {

        if (page <= 0 || size <= 0) {
            // Bad: unbounded query
            List<Movie> movies = service.paginationBad(genre);
            return Map.of("genre", genre, "movies", movies, "method", "unbounded query");
        }

        // Good: paginated with Limit + ExclusiveStartKey
        List<Movie> movies = service.paginationGood(genre, page, size);
        return Map.of("genre", genre, "page", page, "size", size,
                "movies", movies, "method", "paginated (Limit + ExclusiveStartKey)");
    }
}
