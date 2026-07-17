package com.hogwai.jpaprojections.projection;

import java.util.List;

/**
 * Java Record for a Movie with full detail and its actors.
 * <p>
 * This demonstrates assembling a hierarchical DTO from multiple queries
 * in the service layer, since JPQL constructor expressions cannot nest
 * collections in a single query.
 */
public record MovieDetailDto(Long id, String title, int releaseYear, String genre, List<ActorDto> actors) {

    public record ActorDto(Long id, String firstName, String lastName) {
    }
}
