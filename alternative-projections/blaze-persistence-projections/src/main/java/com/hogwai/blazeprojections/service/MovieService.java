package com.hogwai.blazeprojections.service;

import com.hogwai.blazeprojections.dto.GenreStatDto;
import com.hogwai.blazeprojections.repository.MovieRepository;
import com.hogwai.blazeprojections.view.MovieTitleView;
import com.hogwai.blazeprojections.view.MovieWithActorsView;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository repository;

    public MovieService(MovieRepository repository) {
        this.repository = repository;
    }

    public MovieTitleView getTitle(Long id) {
        return repository.findTitleById(id);
    }

    public MovieWithActorsView getWithActors(Long id) {
        return repository.findWithActorsById(id);
    }

    public List<MovieTitleView> findByGenreAndYear(String genre, Integer year) {
        return repository.findByGenreAndYear(genre, year);
    }

    public List<GenreStatDto> getGenreStats() {
        return repository.findGenreStats();
    }
}
