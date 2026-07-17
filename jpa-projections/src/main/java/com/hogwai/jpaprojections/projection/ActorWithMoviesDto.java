package com.hogwai.jpaprojections.projection;

import java.util.List;

/**
 * Java Record DTO for an Actor with their movies.
 * Assembled in the service layer from multiple queries.
 */
public record ActorWithMoviesDto(Long id, String firstName, String lastName, List<MovieTitleDto> movies) {
}
