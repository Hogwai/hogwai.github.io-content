package com.hogwai.nosql.cassandra.controller;

import java.util.List;

import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hogwai.nosql.cassandra.model.Movie;
import com.hogwai.nosql.cassandra.projection.GenreStat;
import com.hogwai.nosql.cassandra.projection.MovieDetailDto;
import com.hogwai.nosql.cassandra.service.CassandraProjectionsService;

@RestController
@RequestMapping("/api/cassandra/movies")
public class CassandraProjectionsController {

    private final CassandraProjectionsService movieService;

    public CassandraProjectionsController(CassandraProjectionsService movieService) {
        this.movieService = movieService;
    }

    @GetMapping
    public List<?> findByGenre(@RequestParam String genre) {
        return movieService.findByGenreProjected(genre);
    }

    // Full entity endpoint (for Gatling comparison with projection)
    @GetMapping("/full")
    public List<Movie> findByGenreFull(@RequestParam String genre) {
        return movieService.findByGenreFull(genre);
    }

    @GetMapping("/search")
    public List<?> searchByTitle(@RequestParam String title) {
        return movieService.searchByTitle(title);
    }

    @GetMapping("/stats/genre")
    public List<GenreStat> statsByGenre(@RequestParam String genre) {
        return movieService.countByGenre(genre);
    }

    @GetMapping("/{id}/detail")
    public MovieDetailDto getDetail(@PathVariable String id) {
        return movieService.getDetailById(id);
    }

    @GetMapping("/paged")
    public Slice<?> findByGenrePaged(
            @RequestParam String genre,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        return movieService.findSliceByGenreProjected(genre, CassandraPageRequest.first(size));
    }
}
