package com.hogwai.querydslprojections.service;

import com.hogwai.querydslprojections.dto.GenreStatDto;
import com.hogwai.querydslprojections.dto.MovieTitleDto;
import com.hogwai.querydslprojections.dto.MovieWithActorsDto;
import com.hogwai.querydslprojections.repository.MovieRepository;
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
