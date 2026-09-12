package com.hogwai.jooqcodegenprojections.service;

import com.hogwai.jooqcodegenprojections.dto.GenreStatDto;
import com.hogwai.jooqcodegenprojections.dto.MovieTitleDto;
import com.hogwai.jooqcodegenprojections.dto.MovieWithActorsDto;
import com.hogwai.jooqcodegenprojections.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository repository;

    public MovieService(MovieRepository repository) {
        this.repository = repository;
    }

    public MovieTitleDto getTitle(Long id) {
        return repository.findTitleById(id);
    }

    public MovieWithActorsDto getWithActors(Long id) {
        return repository.findWithActorsById(id);
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        return repository.findByGenreAndYear(genre, year);
    }

    public List<GenreStatDto> getGenreStats() {
        return repository.findGenreStats();
    }
}
