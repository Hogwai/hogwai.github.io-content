package com.hogwai.nosql.mongodb.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hogwai.nosql.mongodb.model.Movie;
import com.hogwai.nosql.mongodb.projection.MovieDetailDto;
import com.hogwai.nosql.mongodb.projection.MovieTitleView;
import com.hogwai.nosql.mongodb.repository.MovieRepository;

@Service
@Transactional(readOnly = true)
public class MongoProjectionsService {

    private final MovieRepository movieRepository;

    public MongoProjectionsService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieTitleView> findByGenre(String genre) {
        return movieRepository.findByGenre(genre);
    }

    public List<Movie> findByGenreFull(String genre) {
        return movieRepository.findByGenre(genre, Movie.class);
    }

    public List<Movie> findTitleAndYearByGenre(String genre) {
        return movieRepository.findTitleAndYearByGenre(genre);
    }

    public List<MovieTitleView> findProjectedByGenre(String genre) {
        return movieRepository.findProjectedByGenre(genre);
    }

    public List<?> findDynamicByGenre(String genre, String type) {
        if ("detail".equals(type)) {
            return movieRepository.findByGenre(genre, MovieDetailDto.class);
        }
        return movieRepository.findByGenre(genre, MovieTitleView.class);
    }

    public Slice<MovieTitleView> findSliceByGenre(String genre, Pageable pageable) {
        return movieRepository.findSliceByGenre(genre, pageable);
    }

    public Page<Movie> findPageByGenre(String genre, Pageable pageable) {
        return movieRepository.findPageByGenre(genre, pageable);
    }

    public MovieDetailDto getDetailById(String id) {
        Movie movie = movieRepository.findById(id).orElseThrow();
        return new MovieDetailDto(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseYear(),
                movie.getGenre(),
                movie.getActors() != null
                        ? movie.getActors().stream()
                                .map(a -> new MovieDetailDto.ActorDto(
                                        a.getId(),
                                        a.getFirstName(),
                                        a.getLastName(),
                                        a.getBio(),
                                        a.getBirthDate(),
                                        a.getNationality(),
                                        a.getAwards(),
                                        a.getFilmography()))
                                .toList()
                        : List.of()
        );
    }
}
