package com.hogwai.jpaprojections.projection;

import org.springframework.data.annotation.PersistenceCreator;

/**
 * POJO class DTO with multiple constructors, demonstrating {@code @PersistenceCreator}.
 * <p>
 * Spring Data requires an unambiguous constructor for class-based DTO projections.
 * When a class defines more than one constructor, annotate the intended one with
 * {@code @PersistenceCreator}. Records handle this automatically via their canonical
 * constructor and do not need this annotation.
 * <p>
 * The no-arg constructor exists for frameworks that require one (e.g., some serializers),
 * but {@code @PersistenceCreator} tells Spring Data which constructor to use for projection.
 */
public class GenreStatDto {

    private final String genre;
    private final long movieCount;

    /**
     * Framework-friendly no-arg constructor (protected, not intended for direct use).
     */
    protected GenreStatDto() {
        this.genre = null;
        this.movieCount = 0L;
    }

    /**
     * Primary constructor used by Spring Data for DTO projection.
     *
     * @param genre      the movie genre
     * @param movieCount the number of movies in this genre
     */
    @PersistenceCreator
    public GenreStatDto(String genre, long movieCount) {
        this.genre = genre;
        this.movieCount = movieCount;
    }

    public String getGenre() {
        return genre;
    }

    public long getMovieCount() {
        return movieCount;
    }
}
