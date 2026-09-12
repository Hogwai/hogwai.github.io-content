package com.hogwai.querydslcodegenprojections.repository;

import com.hogwai.querydslcodegenprojections.dto.GenreStatDto;
import com.hogwai.querydslcodegenprojections.dto.MovieTitleDto;
import com.hogwai.querydslcodegenprojections.dto.MovieWithActorsDto;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hogwai.querydslcodegenprojections.generated.QActors.actors;
import static com.hogwai.querydslcodegenprojections.generated.QMovies.movies;
import static com.hogwai.querydslcodegenprojections.generated.QMoviesActors.moviesActors;

/**
 * Type-safe QueryDSL SQL projections built on generated Q-classes.
 *
 * <p>The Q-classes are generated from the database schema at build time by the QueryDSL
 * Maven plugin, so their class names, property names and column types come from JDBC
 * metadata rather than being written by hand. This is the main difference with the
 * hand-written sibling project, where the RelationalPathBase classes and their
 * ColumnMetadata are maintained manually.
 */
@Repository
@Transactional(readOnly = true)
public class MovieRepository {

    public static final ConstructorExpression<MovieTitleDto> MOVIE_TITLE_PROJECTION =
            Projections.constructor(MovieTitleDto.class, movies.id, movies.title, movies.releaseYear, movies.genre);
    public static final ConstructorExpression<MovieWithActorsDto.ActorDto> MOVIE_WITH_ACTORS_PROJECTION =
            Projections.constructor(MovieWithActorsDto.ActorDto.class, actors.id, actors.firstName, actors.lastName);
    public static final ConstructorExpression<GenreStatDto> GENRE_STATS_PROJECTION =
            Projections.constructor(GenreStatDto.class, movies.genre, movies.id.count());
    private final SQLQueryFactory queryFactory;

    public MovieRepository(SQLQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public MovieTitleDto findTitleById(Long id) {
        return queryFactory
                .select(MOVIE_TITLE_PROJECTION)
                .from(movies)
                .where(movies.id.eq(id))
                .fetchOne();
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        var query = queryFactory
                .select(MOVIE_TITLE_PROJECTION)
                .from(movies);

        if (genre != null) {
            query = query.where(movies.genre.eq(genre));
        }
        if (year != null) {
            query = query.where(movies.releaseYear.eq(year));
        }

        return query.fetch();
    }

    public List<GenreStatDto> findGenreStats() {
        return queryFactory
                .select(GENRE_STATS_PROJECTION)
                .from(movies)
                .groupBy(movies.genre)
                .orderBy(movies.genre.asc())
                .fetch();
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        MovieTitleDto movieResult = findTitleById(id);
        if (movieResult == null) {
            return null;
        }

        List<MovieWithActorsDto.ActorDto> actorDtos = queryFactory
                .select(MOVIE_WITH_ACTORS_PROJECTION)
                .from(moviesActors)
                .join(actors).on(moviesActors.actorId.eq(actors.id))
                .where(moviesActors.movieId.eq(id))
                .orderBy(actors.id.asc())
                .fetch();

        return new MovieWithActorsDto(
                movieResult.id(),
                movieResult.title(),
                movieResult.releaseYear(),
                movieResult.genre(),
                actorDtos);
    }
}
