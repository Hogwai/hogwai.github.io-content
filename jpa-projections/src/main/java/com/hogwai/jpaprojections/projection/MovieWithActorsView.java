package com.hogwai.jpaprojections.projection;

import java.util.Set;

/**
 * Interface Closed Projection with nested projection for Many-to-Many association.
 * Nested collections require explicit JOIN FETCH in @Query or @EntityGraph
 * to avoid N+1 queries. The nested projection (ActorView) will still SELECT
 * all columns of the joined entity (Hibernate limitation).
 */
public interface MovieWithActorsView {
    Long getId();
    String getTitle();
    int getReleaseYear();
    Set<ActorView> getActors();

    interface ActorView {
        Long getId();
        String getFirstName();
        String getLastName();
    }
}
