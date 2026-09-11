package com.hogwai.nosql.cassandra.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.hogwai.nosql.cassandra.model.Movie;
import com.hogwai.nosql.cassandra.projection.GenreStat;
import com.hogwai.nosql.cassandra.projection.MovieDetailDto;
import com.hogwai.nosql.cassandra.repository.ActorRepository;
import com.hogwai.nosql.cassandra.repository.MovieRepository;

@Service
public class CassandraProjectionsService {

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;

    public CassandraProjectionsService(MovieRepository movieRepository, ActorRepository actorRepository) {
        this.movieRepository = movieRepository;
        this.actorRepository = actorRepository;
    }

    public List<Movie> findByGenreProjected(String genre) {
        return movieRepository.findByGenreProjected(genre);
    }

    public List<Movie> findByGenreFull(String genre) {
        return movieRepository.findByGenreFull(genre);
    }

    public List<Movie> searchByTitle(String title) {
        return movieRepository.searchByTitle(title);
    }

    public List<GenreStat> countByGenre(String genre) {
        return movieRepository.countByGenre(genre);
    }

    public MovieDetailDto getDetailById(String id) {
        Movie movie = movieRepository.findById(id).orElseThrow();

        List<MovieDetailDto.ActorDetailDto> actors = List.of();
        if (movie.getActorIds() != null && !movie.getActorIds().isEmpty()) {
            String[] actorIdArray = movie.getActorIds().split(",");
            actors = Arrays.stream(actorIdArray)
                    .map(actorId -> actorRepository.findById(actorId.trim()).orElse(null))
                    .filter(Objects::nonNull)
                    .map(a -> new MovieDetailDto.ActorDetailDto(
                            a.getId(),
                            a.getFirstName(),
                            a.getLastName(),
                            a.getBio(),
                            a.getBirthDate(),
                            a.getNationality(),
                            a.getAwards() != null ? Arrays.asList(a.getAwards().split(",")) : List.of(),
                            a.getFilmography() != null ? Arrays.asList(a.getFilmography().split(",")) : List.of()
                    ))
                    .toList();
        }

        return new MovieDetailDto(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseYear(),
                movie.getGenre(),
                actors
        );
    }

    public Slice<Movie> findSliceByGenreProjected(String genre, Pageable pageable) {
        return movieRepository.findSliceByGenreProjected(genre, pageable);
    }
}
