package com.hogwai.nosql.cassandra.projection;

public record GenreStat(
        String genre,
        long movieCount
) {
}
