package com.hogwai.querydslprojections.model;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

import java.sql.Types;

/**
 * Relational path for the movies_actors join table
 */
@SuppressWarnings("java:S2160")
public class QMovieActor extends RelationalPathBase<QMovieActor> {

    public static final QMovieActor movieActor = new QMovieActor("movies_actors");

    public final NumberPath<Long> movieId = createNumber("movieId", Long.class);
    public final NumberPath<Long> actorId = createNumber("actorId", Long.class);

    public QMovieActor(String variable) {
        super(QMovieActor.class, variable, "", "movies_actors");
        addMetadata(movieId, ColumnMetadata.named("movie_id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(actorId, ColumnMetadata.named("actor_id").withIndex(2).ofType(Types.BIGINT));
    }
}
