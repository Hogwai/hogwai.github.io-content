package com.hogwai.jooqcodegenprojections.repository;

import com.hogwai.jooqcodegenprojections.dto.GenreStatDto;
import com.hogwai.jooqcodegenprojections.dto.MovieTitleDto;
import com.hogwai.jooqcodegenprojections.dto.MovieWithActorsDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static com.hogwai.jooqcodegenprojections.generated.tables.Actors.ACTORS;
import static com.hogwai.jooqcodegenprojections.generated.tables.Movies.MOVIES;
import static com.hogwai.jooqcodegenprojections.generated.tables.MoviesActors.MOVIES_ACTORS;
import static org.jooq.impl.DSL.count;

@Repository
@Transactional(readOnly = true)
public class MovieRepository {

    public static final String TITLE = "title";
    public static final String RELEASE_YEAR = "releaseYear";
    public static final String GENRE = "genre";
    public static final String ID = "id";

    private final DSLContext dsl;

    public MovieRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public MovieTitleDto findTitleById(Long id) {
        return dsl.select(
                        MOVIES.ID.as(ID),
                        MOVIES.TITLE.as(TITLE),
                        MOVIES.RELEASE_YEAR.as(RELEASE_YEAR),
                        MOVIES.GENRE.as(GENRE))
                .from(MOVIES)
                .where(MOVIES.ID.eq(id))
                .fetchOneInto(MovieTitleDto.class);
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        List<Condition> filters = new ArrayList<>();
        if (genre != null) {
            filters.add(MOVIES.GENRE.eq(genre));
        }
        if (year != null) {
            filters.add(MOVIES.RELEASE_YEAR.eq(year));
        }

        return dsl.select(
                        MOVIES.ID.as(ID),
                        MOVIES.TITLE.as(TITLE),
                        MOVIES.RELEASE_YEAR.as(RELEASE_YEAR),
                        MOVIES.GENRE.as(GENRE))
                .from(MOVIES)
                .where(filters)
                .orderBy(MOVIES.ID.asc())
                .fetchInto(MovieTitleDto.class);
    }

    public List<GenreStatDto> findGenreStats() {
        return dsl.select(
                        MOVIES.GENRE.as(GENRE),
                        count(MOVIES.ID).cast(Long.class).as("movieCount"))
                .from(MOVIES)
                .groupBy(MOVIES.GENRE)
                .orderBy(MOVIES.GENRE.asc())
                .fetchInto(GenreStatDto.class);
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        MovieTitleDto movie = findTitleById(id);
        if (movie == null) {
            return null;
        }

        List<MovieWithActorsDto.ActorDto> actors = dsl.select(
                        ACTORS.ID.as(ID),
                        ACTORS.FIRST_NAME.as("firstName"),
                        ACTORS.LAST_NAME.as("lastName"))
                .from(ACTORS)
                .join(MOVIES_ACTORS).on(ACTORS.ID.eq(MOVIES_ACTORS.ACTOR_ID))
                .where(MOVIES_ACTORS.MOVIE_ID.eq(id))
                .orderBy(ACTORS.ID.asc())
                .fetchInto(MovieWithActorsDto.ActorDto.class);

        return new MovieWithActorsDto(
                movie.id(),
                movie.title(),
                movie.releaseYear(),
                movie.genre(),
                actors);
    }
}
