package com.hogwai.jdbiprojections.service;

import com.hogwai.jdbiprojections.dto.GenreStatDto;
import com.hogwai.jdbiprojections.dto.MovieTitleDto;
import com.hogwai.jdbiprojections.dto.MovieWithActorsDto;
import com.hogwai.jdbiprojections.repository.MovieFluentRepository;
import com.hogwai.jdbiprojections.repository.MovieSqlObject;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieSqlObject sqlObject;
    private final MovieFluentRepository fluentRepository;

    public MovieService(MovieSqlObject sqlObject, MovieFluentRepository fluentRepository) {
        this.sqlObject = sqlObject;
        this.fluentRepository = fluentRepository;
    }

    public MovieTitleDto getTitle(Long id) {
        return sqlObject.findTitleById(id);
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        return sqlObject.findByGenreAndYear(genre, year);
    }

    public List<GenreStatDto> getGenreStats() {
        return sqlObject.findGenreStats();
    }

    public MovieWithActorsDto getWithActors(Long id) {
        return fluentRepository.findWithActorsById(id);
    }
}
