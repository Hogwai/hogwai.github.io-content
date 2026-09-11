package com.hogwai.nosql.mongodb.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hogwai.nosql.mongodb.model.Movie;
import com.hogwai.nosql.mongodb.projection.MovieDetailDto;
import com.hogwai.nosql.mongodb.projection.MovieTitleView;
import com.hogwai.nosql.mongodb.service.MongoProjectionsService;

@RestController
@RequestMapping("/api/mongo/movies")
public class MongoProjectionsController {

    private final MongoProjectionsService movieService;

    public MongoProjectionsController(MongoProjectionsService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<MovieTitleView> findByGenre(@RequestParam String genre) {
        return movieService.findByGenre(genre);
    }

    // Full entity endpoint (for Gatling comparison with projection)
    @GetMapping("/full")
    public List<Movie> findByGenreFull(@RequestParam String genre) {
        return movieService.findByGenreFull(genre);
    }

    @GetMapping("/search")
    public List<?> searchByGenre(@RequestParam String genre) {
        return movieService.findTitleAndYearByGenre(genre);
    }

    @GetMapping("/stats/genre")
    public List<MovieTitleView> statsByGenre(@RequestParam String genre) {
        return movieService.findProjectedByGenre(genre);
    }

    @GetMapping("/dynamic")
    public List<?> dynamicProjection(
            @RequestParam String genre,
            @RequestParam(defaultValue = "title") String type) {
        return movieService.findDynamicByGenre(genre, type);
    }

    @GetMapping("/{id}/detail")
    public MovieDetailDto getDetail(@PathVariable String id) {
        return movieService.getDetailById(id);
    }

    // Slice pagination: lightweight, no count query

    @GetMapping("/paged/slice")
    public Slice<MovieTitleView> findByGenrePaged(
            @RequestParam String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return movieService.findSliceByGenre(genre, PageRequest.of(page, size));
    }

    // Page pagination: includes total count

    @GetMapping("/paged/page")
    public Page<Movie> findByGenrePagedWithCount(
            @RequestParam String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return movieService.findPageByGenre(genre, PageRequest.of(page, size));
    }
}
