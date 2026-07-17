package com.hogwai.jpaprojections.projection;

/**
 * Interface Closed Projection with default method (preferred over @Value SpEL).
 * <p>
 * Best practice: use default methods instead of @Value("#{target.firstName + ' ' + target.lastName}")
 * because @Value makes the projection "open" and disables query optimization (full entity loaded).
 * Default methods keep the projection "closed", Spring Data still optimizes the SELECT.
 */
public interface ActorNameView {
    Long getId();
    String getFirstName();
    String getLastName();

    default String getFullName() {
        return getFirstName() + " " + getLastName();
    }
}
