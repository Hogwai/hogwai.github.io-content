package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GsiVsFilterController {

    private final MovieService service;

    public GsiVsFilterController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies/by-author")
    public Map<String, Object> getMoviesByAuthor(
            @RequestParam String author,
            @RequestParam(required = false) String genre) {

        if (genre != null && !genre.isEmpty()) {
            // Bad: filter on non-key attribute
            List<Movie> movies = service.gsiVsFilterBad(genre, author);
            return Map.of("author", author, "genre", genre, "movies", movies,
                    "method", "FilterExpression on partition");
        }

        // Good: GSI query
        List<Movie> movies = service.gsiVsFilterGood(author);
        return Map.of("author", author, "movies", movies,
                "method", "GSI query");
    }
}
