package com.hogwai.jdbctemplateprojections.repository;

import com.hogwai.jdbctemplateprojections.dto.GenreStatDto;
import com.hogwai.jdbctemplateprojections.dto.MovieTitleDto;
import com.hogwai.jdbctemplateprojections.dto.MovieWithActorsDto;
import com.hogwai.jdbctemplateprojections.mappers.GenreStatMapper;
import com.hogwai.jdbctemplateprojections.mappers.MovieTitleMapper;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class MovieRepository {

    private static final String SELECT_MOVIES = "SELECT id, title, release_year, genre FROM movies";
    public static final String SELECT_COUNT_GENRE_QUERY =
            "SELECT genre, COUNT(*) as movie_count FROM movies GROUP BY genre";
    public static final String FIND_WITH_ACTORS_BY_ID = """
            SELECT m.id, m.title, m.release_year, m.genre,
                   a.id as actor_id, a.first_name, a.last_name
            FROM movies m
            LEFT JOIN movies_actors ma ON m.id = ma.movie_id
            LEFT JOIN actors a ON ma.actor_id = a.id
            WHERE m.id = :id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final MovieTitleMapper movieTitleMapper;
    private final GenreStatMapper genreStatMapper;

    public MovieRepository(NamedParameterJdbcTemplate jdbcTemplate,
                           MovieTitleMapper movieTitleMapper,
                           GenreStatMapper genreStatMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.movieTitleMapper = movieTitleMapper;
        this.genreStatMapper = genreStatMapper;
    }

    public MovieTitleDto findTitleById(Long id) {
        return jdbcTemplate.queryForObject(
                SELECT_MOVIES + " WHERE id = :id",
                new MapSqlParameterSource("id", id),
                movieTitleMapper
        );
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        List<String> conditions = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (genre != null) {
            conditions.add("genre = :genre");
            params.addValue("genre", genre);
        }
        if (year != null) {
            conditions.add("release_year = :year");
            params.addValue("year", year);
        }

        String sql = SELECT_MOVIES;
        if (!conditions.isEmpty()) {
            sql = "%s WHERE %s".formatted(SELECT_MOVIES, String.join(" AND ", conditions));
        }

        return jdbcTemplate.query(sql, params, movieTitleMapper);
    }

    public List<GenreStatDto> findGenreStats() {
        return jdbcTemplate.query(SELECT_COUNT_GENRE_QUERY, genreStatMapper);
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        ResultSetExtractor<MovieWithActorsDto> extractor = rs -> {
            MovieWithActorsDto movie = null;
            List<MovieWithActorsDto.ActorDto> actors = new ArrayList<>();
            while (rs.next()) {
                if (movie == null) {
                    movie = new MovieWithActorsDto(
                            rs.getLong("id"),
                            rs.getString("title"),
                            rs.getInt("release_year"),
                            rs.getString("genre"),
                            actors
                    );
                }
                long actorId = rs.getLong("actor_id");
                if (!rs.wasNull()) {
                    actors.add(new MovieWithActorsDto.ActorDto(
                            actorId,
                            rs.getString("first_name"),
                            rs.getString("last_name")
                    ));
                }
            }
            return movie;
        };

        return jdbcTemplate.query(FIND_WITH_ACTORS_BY_ID, new MapSqlParameterSource("id", id), extractor);
    }
}
