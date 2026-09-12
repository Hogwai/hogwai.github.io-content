package com.hogwai.querydslprojections.model;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

import java.sql.Types;

/**
 * Relational path for the movies table
 */
@SuppressWarnings("java:S2160")
public class QMovie extends RelationalPathBase<QMovie> {

    public static final QMovie movie = new QMovie("movies");

    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath title = createString("title");
    public final NumberPath<Integer> releaseYear = createNumber("releaseYear", Integer.class);
    public final StringPath genre = createString("genre");

    public QMovie(String variable) {
        super(QMovie.class, variable, "", "movies");
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(title, ColumnMetadata.named("title").withIndex(2).ofType(Types.VARCHAR));
        addMetadata(releaseYear, ColumnMetadata.named("release_year").withIndex(3).ofType(Types.INTEGER));
        addMetadata(genre, ColumnMetadata.named("genre").withIndex(4).ofType(Types.VARCHAR));
    }
}
