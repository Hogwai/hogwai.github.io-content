package com.hogwai.querydslprojections.model;

import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.sql.ColumnMetadata;
import com.querydsl.sql.RelationalPathBase;

import java.sql.Types;

/**
 * Relational path for the actors table.
 */
@SuppressWarnings("java:S2160")
public class QActor extends RelationalPathBase<QActor> {

    public static final QActor actor = new QActor("actors");

    public final NumberPath<Long> id = createNumber("id", Long.class);
    public final StringPath firstName = createString("firstName");
    public final StringPath lastName = createString("lastName");

    public QActor(String variable) {
        super(QActor.class, variable, "", "actors");
        addMetadata(id, ColumnMetadata.named("id").withIndex(1).ofType(Types.BIGINT));
        addMetadata(firstName, ColumnMetadata.named("first_name").withIndex(2).ofType(Types.VARCHAR));
        addMetadata(lastName, ColumnMetadata.named("last_name").withIndex(3).ofType(Types.VARCHAR));
    }
}
