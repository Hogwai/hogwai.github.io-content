package com.hogwai.jpaprojections.projection;

import java.util.Optional;

/**
 * Interface Closed Projection: limits SELECT to declared accessors.
 * <p>Spring Data optimizes the query to SELECT only the columns matching the getter names.
 * Nullable columns can be declared as {@link Optional} wrappers — Spring Data returns
 * {@link Optional#empty()} when the database value is {@code null}.
 */
public interface MovieTitleView {
    Long getId();
    String getTitle();

    /**
     * Returns the genre wrapped in an {@link Optional}.
     * <p>Spring Data supports nullable wrappers ({@link Optional}, nullable types)
     * as return types in interface projections. When the database column is NULL,
     * the projection returns {@link Optional#empty()} instead of {@code null}.
     */
    Optional<String> getGenre();
}
