package com.hogwai.jooqprojections.repository;

import com.hogwai.jooqprojections.dto.GenreStatDto;
import com.hogwai.jooqprojections.dto.MovieTitleDto;
import com.hogwai.jooqprojections.dto.MovieWithActorsDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

/**
 * jOOQ plain SQL projections. Because this project does not run jOOQ code
 * generation, the tables and columns are declared by hand as {@link Table} and
 * {@link Field} constants. Selecting only the needed columns and aliasing them
 * to the record component names lets jOOQ map each row straight into a DTO, so
 * no entity hydration or lazy loading is involved.
 */
@Repository
@Transactional(readOnly = true)
public class MovieRepository {

    private static final Table<?> MOVIES = table("movies");
    private static final Table<?> ACTORS = table("actors");
    private static final Table<?> MOVIES_ACTORS = table("movies_actors");

    private static final Field<Long> MOVIE_ID = field("movies.id", Long.class);
    private static final Field<String> MOVIE_TITLE = field("movies.title", String.class);
    private static final Field<Integer> MOVIE_RELEASE_YEAR = field("movies.release_year", Integer.class);
    private static final Field<String> MOVIE_GENRE = field("movies.genre", String.class);

    private static final Field<Long> ACTOR_ID = field("actors.id", Long.class);
    private static final Field<String> ACTOR_FIRST_NAME = field("actors.first_name", String.class);
    private static final Field<String> ACTOR_LAST_NAME = field("actors.last_name", String.class);
    private static final Field<Long> MOVIE_ACTOR_ACTOR_ID = field("movies_actors.actor_id", Long.class);
    private static final Field<Long> MOVIE_ACTOR_MOVIE_ID = field("movies_actors.movie_id", Long.class);

    private final DSLContext dsl;

    public MovieRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public MovieTitleDto findTitleById(Long id) {
        return dsl.select(
                        MOVIE_ID.as("id"),
                        MOVIE_TITLE.as("title"),
                        MOVIE_RELEASE_YEAR.as("releaseYear"),
                        MOVIE_GENRE.as("genre"))
                .from(MOVIES)
                .where(MOVIE_ID.eq(id))
                .fetchOneInto(MovieTitleDto.class);
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        List<Condition> filters = new ArrayList<>();
        if (genre != null) {
            filters.add(MOVIE_GENRE.eq(genre));
        }
        if (year != null) {
            filters.add(MOVIE_RELEASE_YEAR.eq(year));
        }

        return dsl.select(
                        MOVIE_ID.as("id"),
                        MOVIE_TITLE.as("title"),
                        MOVIE_RELEASE_YEAR.as("releaseYear"),
                        MOVIE_GENRE.as("genre"))
                .from(MOVIES)
                .where(filters)
                .orderBy(MOVIE_ID.asc())
                .fetchInto(MovieTitleDto.class);
    }

    public List<GenreStatDto> findGenreStats() {
        return dsl.select(
                        MOVIE_GENRE.as("genre"),
                        count(MOVIE_ID).cast(Long.class).as("movieCount"))
                .from(MOVIES)
                .groupBy(MOVIE_GENRE)
                .orderBy(MOVIE_GENRE.asc())
                .fetchInto(GenreStatDto.class);
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        MovieTitleDto movie = findTitleById(id);
        if (movie == null) {
            return null;
        }

        List<MovieWithActorsDto.ActorDto> actors = dsl.select(
                        ACTOR_ID.as("id"),
                        ACTOR_FIRST_NAME.as("firstName"),
                        ACTOR_LAST_NAME.as("lastName"))
                .from(ACTORS)
                .join(MOVIES_ACTORS).on(ACTOR_ID.eq(MOVIE_ACTOR_ACTOR_ID))
                .where(MOVIE_ACTOR_MOVIE_ID.eq(id))
                .orderBy(ACTOR_ID.asc())
                .fetchInto(MovieWithActorsDto.ActorDto.class);

        return new MovieWithActorsDto(
                movie.id(),
                movie.title(),
                movie.releaseYear(),
                movie.genre(),
                actors);
    }
}
