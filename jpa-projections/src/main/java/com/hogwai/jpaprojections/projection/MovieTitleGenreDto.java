package com.hogwai.jpaprojections.projection;

/**
 * Record DTO for multi-select query rewriting demonstration.
 * <p>
 * When a {@code @Query} returns multiple columns like {@code SELECT m.title, m.genre FROM Movie m},
 * Spring Data automatically rewrites the query to a JPQL constructor expression:
 * {@code SELECT new MovieTitleGenreDto(m.title, m.genre) FROM Movie m}.
 * <p>
 * The constructor parameter names ({@code title}, {@code genre}) must match the
 * property path in the multi-select clause.
 */
public record MovieTitleGenreDto(String title, String genre) {
}
