package com.hogwai.jpaprojections.projection;

/**
 * Java Record DTO Projection
 * <p>
 * Immutable, zero boilerplate (constructor, equals, hashCode, toString auto-generated).
 * Spring Data automatically rewrites queries to SELECT new MovieTitleDto(m.id, m.title)
 * when this record is used as a return type.
 */
public record MovieTitleDto(Long id, String title) {
}
