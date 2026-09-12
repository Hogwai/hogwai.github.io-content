package com.hogwai.jdbctemplateprojections.controller;

import com.hogwai.jdbctemplateprojections.dto.GenreStatDto;
import com.hogwai.jdbctemplateprojections.dto.MovieTitleDto;
import com.hogwai.jdbctemplateprojections.dto.MovieWithActorsDto;
import com.hogwai.jdbctemplateprojections.service.MovieService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MoviesController {

    private final MovieService service;

    public MoviesController(MovieService service) {
        this.service = service;
    }

    @GetMapping("/movies/{id}")
    public MovieTitleDto getById(@PathVariable Long id) {
        return service.getTitle(id);
    }

    @GetMapping("/movies/{id}/full")
    public MovieWithActorsDto getFullById(@PathVariable Long id) {
        return service.getWithActors(id);
    }

    @GetMapping("/movies")
    public List<MovieTitleDto> search(
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) Integer year) {
        return service.findByGenreAndYear(genre, year);
    }

    @GetMapping("/movies/stats")
    public List<GenreStatDto> stats() {
        return service.getGenreStats();
    }
}
