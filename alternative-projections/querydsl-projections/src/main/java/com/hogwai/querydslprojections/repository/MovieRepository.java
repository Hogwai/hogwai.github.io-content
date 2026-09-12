package com.hogwai.querydslprojections.repository;

import com.hogwai.querydslprojections.dto.GenreStatDto;
import com.hogwai.querydslprojections.dto.MovieTitleDto;
import com.hogwai.querydslprojections.dto.MovieWithActorsDto;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.core.types.Projections;
import com.querydsl.sql.SQLQueryFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.hogwai.querydslprojections.model.QActor.actor;
import static com.hogwai.querydslprojections.model.QMovie.movie;
import static com.hogwai.querydslprojections.model.QMovieActor.movieActor;

/**
 * Type-safe QueryDSL SQL projections
 */
@Repository
@Transactional(readOnly = true)
public class MovieRepository {

    public static final ConstructorExpression<MovieTitleDto> MOVIE_TITLE_PROJECTION =
            Projections.constructor(MovieTitleDto.class, movie.id, movie.title, movie.releaseYear, movie.genre);
    public static final ConstructorExpression<MovieWithActorsDto.ActorDto> MOVIE_WITH_ACTORS_PROJECTION =
            Projections.constructor(MovieWithActorsDto.ActorDto.class, actor.id, actor.firstName, actor.lastName);
    public static final ConstructorExpression<GenreStatDto> GENRE_STATS_PROJECTION =
            Projections.constructor(GenreStatDto.class, movie.genre, movie.id.count());
    private final SQLQueryFactory queryFactory;

    public MovieRepository(SQLQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    public MovieTitleDto findTitleById(Long id) {
        return queryFactory
                .select(MOVIE_TITLE_PROJECTION)
                .from(movie)
                .where(movie.id.eq(id))
                .fetchOne();
    }

    public List<MovieTitleDto> findByGenreAndYear(String genre, Integer year) {
        var query = queryFactory
                .select(MOVIE_TITLE_PROJECTION)
                .from(movie);

        if (genre != null) {
            query = query.where(movie.genre.eq(genre));
        }
        if (year != null) {
            query = query.where(movie.releaseYear.eq(year));
        }

        return query.fetch();
    }

    public List<GenreStatDto> findGenreStats() {
        return queryFactory
                .select(GENRE_STATS_PROJECTION)
                .from(movie)
                .groupBy(movie.genre)
                .orderBy(movie.genre.asc())
                .fetch();
    }

    public MovieWithActorsDto findWithActorsById(Long id) {
        MovieTitleDto movieResult = findTitleById(id);
        if (movieResult == null) {
            return null;
        }

        List<MovieWithActorsDto.ActorDto> actors = queryFactory
                .select(MOVIE_WITH_ACTORS_PROJECTION)
                .from(movieActor)
                .join(actor).on(movieActor.actorId.eq(actor.id))
                .where(movieActor.movieId.eq(id))
                .orderBy(actor.id.asc())
                .fetch();

        return new MovieWithActorsDto(
                movieResult.id(),
                movieResult.title(),
                movieResult.releaseYear(),
                movieResult.genre(),
                actors);
    }
}
