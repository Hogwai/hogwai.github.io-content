package com.hogwai.jpaprojections.helper;

import com.hogwai.jpaprojections.entity.Movie;
import org.springframework.stereotype.Component;

/**
 * Helper bean referenced by {@code @Value} SpEL expressions in open projections.
 * <p>
 * When a projection needs a computed value that depends on a Spring bean (e.g.
 * formatting, external lookup), the {@code @Value("#{@beanName.method(target)}")}
 * annotation is the standard approach. Note that using {@code @Value} makes the
 * projection <em>open</em>, disabling SELECT optimisation: the full entity is loaded.
 */
@Component
public class ProjectionHelper {

    /**
     * Formats a movie's title with its genre in brackets.
     * <p>Example: "The Matrix [Sci-Fi]"
     *
     * @param movie the Movie entity (passed as {@code target} in the SpEL expression)
     * @return formatted genre label
     */
    public String formatGenreLabel(Movie movie) {
        return movie.getTitle() + " [" + movie.getGenre() + "]";
    }
}
