package com.hogwai.jpaprojections.repository;

import com.hogwai.jpaprojections.entity.Movie;
import org.springframework.data.jpa.domain.Specification;

/**
 * Reusable {@link Specification} factories for the {@link Movie} entity.
 * <p>Used in combination with the fluent {@code findBy} API to demonstrate
 * dynamic predicate composition with projections.
 */
public class MovieSpecifications {

    public static Specification<Movie> genreEquals(String genre) {
        return (root, query, cb) -> cb.equal(root.get("genre"), genre);
    }

    public static Specification<Movie> releaseYearGreaterOrEqual(int year) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("releaseYear"), year);
    }

    public static Specification<Movie> titleContains(String title) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("title")),
                "%" + title.toLowerCase() + "%");
    }

    /**
     * Combine multiple specifications with AND.
     * <p>Usage: {@code Specification<Movie> spec = genreEquals("Sci-Fi").and(releaseYearGte(2000));}
     */
    private MovieSpecifications() {
    }
}
