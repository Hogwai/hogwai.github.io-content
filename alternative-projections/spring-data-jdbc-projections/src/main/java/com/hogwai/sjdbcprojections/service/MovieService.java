package com.hogwai.sjdbcprojections.service;

import com.hogwai.sjdbcprojections.dto.GenreStatDto;
import com.hogwai.sjdbcprojections.dto.MovieTitleDto;
import com.hogwai.sjdbcprojections.dto.MovieWithActorsDto;
import com.hogwai.sjdbcprojections.repository.MovieRepository;
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
        MovieTitleDto movie = repository.findTitleById(id);
        if (movie == null) {
            return null;
        }
        List<MovieWithActorsDto.ActorDto> actors = repository.findActorsByMovieId(id);
        return new MovieWithActorsDto(movie.id(), movie.title(), movie.releaseYear(), movie.genre(), actors);
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        return repository.findByGenreAndYear(genre, year);
    }

    public List<GenreStatDto> getGenreStats() {
        return repository.findGenreStats();
    }
}
