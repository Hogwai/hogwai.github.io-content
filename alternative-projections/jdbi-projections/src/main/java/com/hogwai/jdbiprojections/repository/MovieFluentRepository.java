package com.hogwai.jdbiprojections.repository;

import com.hogwai.jdbiprojections.dto.GenreStatDto;
import com.hogwai.jdbiprojections.dto.MovieTitleDto;
import com.hogwai.jdbiprojections.dto.MovieWithActorsDto;
import org.jdbi.v3.core.Jdbi;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MovieFluentRepository {

    private static final String SELECT_MOVIES = "SELECT id, title, release_year, genre FROM movies";
    private static final String FIND_GENRE_STATS_QUERY =
            "SELECT genre, COUNT(*) as movie_count FROM movies GROUP BY genre";
    private static final String FIND_MOVIE_BY_ID =
            "SELECT id, title, release_year, genre FROM movies WHERE id = :id";
    public static final String FIND_ACTORS_OF_MOVIE =
                    """
                    SELECT a.id, a.first_name, a.last_name
                    FROM actors a JOIN movies_actors ma ON a.id = ma.actor_id
                    WHERE ma.movie_id = :movieId
                    """;

    private final Jdbi jdbi;

    public MovieFluentRepository(Jdbi jdbi) {
        this.jdbi = jdbi;
    }

    public MovieTitleDto findTitleById(Long id) {
        return jdbi.withHandle(handle ->
                handle.createQuery(SELECT_MOVIES + " WHERE id = :id")
                        .bind("id", id)
                        .mapTo(MovieTitleDto.class)
                        .one()
        );
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        return jdbi.withHandle(handle -> {
            List<String> conditions = new ArrayList<>();
            if (genre != null) {
                conditions.add("genre = :genre");
            }
            if (year != null) {
                conditions.add("release_year = :year");
            }

            String sql = "";
            if (!conditions.isEmpty()) {
                sql = "%s WHERE %s".formatted(SELECT_MOVIES, String.join(" AND ", conditions));
            }

            var query = handle.createQuery(sql);
            if (genre != null) {
                query.bind("genre", genre);
            }
            if (year != null) {
                query.bind("year", year);
            }

            return query.mapTo(MovieTitleDto.class).list();
        });
    }

    public List<GenreStatDto> findGenreStats() {
        return jdbi.withHandle(handle ->
                handle.createQuery(FIND_GENRE_STATS_QUERY)
                        .mapTo(GenreStatDto.class)
                        .list()
        );
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        return jdbi.withHandle(handle -> {
            var movie = handle.createQuery(FIND_MOVIE_BY_ID)
                    .bind("id", id)
                    .mapTo(MovieTitleDto.class)
                    .one();

            if (movie == null) {
                return null;
            }

            var actors = handle.createQuery(FIND_ACTORS_OF_MOVIE)
                    .bind("movieId", id)
                    .mapTo(MovieWithActorsDto.ActorDto.class)
                    .list();

            return new MovieWithActorsDto(movie.id(), movie.title(), movie.releaseYear(), movie.genre(), actors);
        });
    }
}
