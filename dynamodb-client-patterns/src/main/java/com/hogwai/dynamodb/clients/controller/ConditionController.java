package com.hogwai.dynamodb.clients.controller;

import com.hogwai.dynamodb.clients.model.Movie;
import com.hogwai.dynamodb.clients.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConditionController {

    private final MovieService service;

    public ConditionController(MovieService service) {
        this.service = service;
    }

    @PutMapping("/movies/unsafe")
    public Map<String, Object> conditionNaive(@RequestParam String genre,
                                               @RequestParam String movieId) {
        Movie result = service.conditionBad(genre, movieId);
        return Map.of("genre", genre, "movieId", movieId,
                "created", result != null, "method", "getItem+putItem");
    }

    @PutMapping("/movies")
    public Map<String, Object> conditionGood(@RequestBody Movie movie) {
        Movie result = service.conditionGood(movie);
        return Map.of("genre", movie.getGenre(), "movieId", movie.getMovieId(),
                "created", result != null, "method", "conditionExpression");
    }
}
