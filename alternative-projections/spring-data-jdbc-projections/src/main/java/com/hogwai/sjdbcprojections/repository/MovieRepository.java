package com.hogwai.sjdbcprojections.repository;

import com.hogwai.sjdbcprojections.dto.GenreStatDto;
import com.hogwai.sjdbcprojections.dto.MovieTitleDto;
import com.hogwai.sjdbcprojections.dto.MovieWithActorsDto;
import com.hogwai.sjdbcprojections.entity.Movie;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieRepository extends CrudRepository<Movie, Long> {

    @Query("SELECT id, title, release_year, genre FROM movies WHERE id = :id")
    MovieTitleDto findTitleById(@Param("id") Long id);

    @Query("SELECT id, title, release_year, genre FROM movies WHERE (:genre IS NULL OR genre = :genre) AND (:year IS NULL OR release_year = :year)")
    List<MovieTitleDto> findByGenreAndYear(@Param("genre") String genre, @Param("year") Integer year);

    @Query("SELECT genre, COUNT(*) as movie_count FROM movies GROUP BY genre")
    List<GenreStatDto> findGenreStats();

    @Query("SELECT a.id, a.first_name, a.last_name FROM actors a JOIN movies_actors ma ON a.id = ma.actor_id WHERE ma.movie_id = :movieId")
    List<MovieWithActorsDto.ActorDto> findActorsByMovieId(@Param("movieId") Long movieId);
}
