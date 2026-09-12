package com.hogwai.blazeprojections.controller;

import com.hogwai.blazeprojections.dto.GenreStatDto;
import com.hogwai.blazeprojections.service.MovieService;
import com.hogwai.blazeprojections.view.MovieTitleView;
import com.hogwai.blazeprojections.view.MovieWithActorsView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MoviesController {

    private final MovieService service;

    public MoviesController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies/{id}")
    public MovieTitleView getById(@PathVariable Long id) {
        return service.getTitle(id);
    }

    @GetMapping("/movies/{id}/full")
    public MovieWithActorsView getFullById(@PathVariable Long id) {
        return service.getWithActors(id);
    }

    @GetMapping("/movies")
    public List<MovieTitleView> search(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year) {
        return service.findByGenreAndYear(genre, year);
    }

    @GetMapping("/movies/stats")
    public List<GenreStatDto> stats() {
        return service.getGenreStats();
    }
}
