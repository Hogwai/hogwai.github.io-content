package com.hogwai.jpaprojections.repository;

import com.hogwai.jpaprojections.entity.Actor;
import com.hogwai.jpaprojections.projection.ActorNameView;
import com.hogwai.jpaprojections.projection.MovieTitleDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Long> {

    /**
     * Interface Closed Projection with a default method.
     * <p>{@link ActorNameView} uses a {@code default getFullName()} method to compute
     * the full name from {@code firstName} and {@code lastName}. Because the interface
     * stays <em>closed</em>, Spring Data still optimises the SELECT to only the needed
     * columns unlike {@code @Value} / SpEL which would defeat the optimisation.
     *
     * @param lastName the actor's last name filter
     * @return actors matching the last name with their computed full name
     */
    List<ActorNameView> findByLastName(String lastName);

    /**
     * Cross-entity query using a Record DTO: finds all movies for a given actor.
     * <p>This demonstrates that a repository can return projections from entities it
     * does not directly manage, as long as the query navigates the relationship.
     *
     * @param actorId the actor's database identifier
     * @return movies associated with the given actor as {@link MovieTitleDto} records
     */
    @Query("""
            SELECT new com.hogwai.jpaprojections.projection.MovieTitleDto(m.id, m.title)
            FROM Movie m JOIN m.actors a
            WHERE a.id = :actorId
            """)
    List<MovieTitleDto> findMoviesByActorId(@Param("actorId") Long actorId);

    /**
     * Dynamic Projection: the caller selects the return type at runtime.
     * <p>Accepts {@link ActorNameView}, {@link Actor}, or any other compatible projection.
     *
     * @param lastName the actor's last name filter
     * @param type     the projection class to return
     * @param <T>      the projection type
     * @return actors in the requested projection form
     */
    <T> List<T> findByLastName(String lastName, Class<T> type);
}
