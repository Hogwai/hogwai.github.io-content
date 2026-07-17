package com.hogwai.jpaprojections.service;

import com.hogwai.jpaprojections.entity.Movie;
import com.hogwai.jpaprojections.projection.*;
import com.hogwai.jpaprojections.repository.ActorRepository;
import com.hogwai.jpaprojections.repository.MovieRepository;
import com.hogwai.jpaprojections.repository.MovieSpecifications;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer demonstrating every projection type featured in this project.
 * <p>
 * Interface projections are best for simple read-only views: Spring Data optimizes
 * the SELECT to only the needed columns. Java Records are the standard for explicit
 * DTOs and work well with JPQL constructor expressions or service-level assembly.
 */
@Service
@Transactional(readOnly = true)
public class ProjectionsDemoService {

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;

    public ProjectionsDemoService(MovieRepository movieRepository, ActorRepository actorRepository) {
        this.movieRepository = movieRepository;
        this.actorRepository = actorRepository;
    }

    /**
     * Interface Closed Projection : Spring Data optimises the SELECT to only the columns
     * declared in {@link MovieTitleView}. Backed by a JDK dynamic proxy wrapping a Tuple.
     */
    public List<MovieTitleView> getMovieTitlesByGenre(String genre) {
        return movieRepository.findByGenre(genre);
    }

    /**
     * Interface Closed Projection with pagination : demonstrates that Spring Data
     * generates the correct LIMIT/OFFSET and count query while still narrowing the
     * SELECT to the projection columns.
     */
    public Page<MovieTitleView> getMovieTitlesByGenrePaged(String genre, Pageable pageable) {
        return movieRepository.findByGenre(genre, pageable);
    }

    /**
     * Nested Interface projection via explicit {@code JOIN FETCH} : avoids N+1 queries
     * on the actors collection. Note: the {@code @Query("SELECT m ...")} loads the full
     * Movie entity, so column narrowing only applies for derived queries.
     *
     * @see #getMoviesWithActorsByGenreEntityGraph(String) alternative with @EntityGraph
     * @see #getMoviesWithActorsNPlusOne(String) the unsafe version (educational)
     */
    public List<MovieWithActorsView> getMoviesWithActorsByGenre(String genre) {
        return movieRepository.findByGenreWithActors(genre);
    }

    /**
     * Nested Interface projection via {@code @EntityGraph} : alternative to JOIN FETCH.
     * The fetch plan is declared on the entity ({@code @NamedEntityGraph}) and reused
     * across queries. Same limitation: full entity columns are selected.
     */
    public List<MovieWithActorsView> getMoviesWithActorsByGenreEntityGraph(String genre) {
        return movieRepository.findByGenreIgnoreCase(genre);
    }

    /**
     * N+1 demonstration : intentionally unsafe. This query lacks {@code JOIN FETCH}
     * or {@code @EntityGraph}, so accessing the nested {@code actors} collection on each
     * result triggers a separate query. Enable {@code show-sql} to observe the difference.
     *
     * @see #getMoviesWithActorsByGenre(String) the safe JOIN FETCH version
     */
    public List<MovieWithActorsView> getMoviesWithActorsNPlusOne(String genre) {
        return movieRepository.findByGenreWithActorsNPlusOne(genre);
    }

    /**
     * Interface projection with a default method : the computed {@code fullName} field
     * is derived from {@code firstName} and {@code lastName} without {@code @Value} SpEL,
     * keeping the projection closed and the SELECT optimised.
     */
    public List<ActorNameView> getActorsByLastName(String lastName) {
        return actorRepository.findByLastName(lastName);
    }

    /**
     * Record DTO via derived query : Spring Data inspects the Record's canonical
     * constructor and generates {@code SELECT new MovieTitleDto(m.id, m.title) ...}
     * automatically. The constructor parameter names must match entity property names
     * (ensured by the {@code -parameters} compiler flag).
     */
    public List<MovieTitleDto> searchMoviesByTitle(String title) {
        return movieRepository.findByTitleContaining(title);
    }

    /**
     * Record DTO with explicit JPQL constructor expression : {@code SELECT NEW} in
     * the {@code @Query} gives full control over the projection columns. Use this
     * when the derived query auto-mapping doesn't fit.
     */
    public List<MovieTitleDto> getMoviesReleasedAfter(int year) {
        return movieRepository.findMoviesReleasedAfter(year);
    }

    /**
     * Aggregation Record : GROUP BY queries return a custom Record DTO.
     * JPQL constructor expression maps each result row to a {@link GenreStat} instance.
     */
    public List<GenreStat> getGenreStatistics() {
        return movieRepository.countByGenre();
    }

    /**
     * Dynamic Projection : the same repository method returns different projection types
     * depending on the {@code Class} argument. Allows callers to choose the view level:
     * entity, interface, or Record DTO.
     */
    public <T> List<T> getMoviesByGenre(String genre, Class<T> projectionType) {
        return movieRepository.findByGenre(genre, projectionType);
    }

    /**
     * Native SQL query with interface projection : column aliases must match the
     * getter names in the interface. Works with the same dynamic proxy mechanism
     * as JPQL-derived projections.
     */
    public List<MovieTitleView> getMovieTitlesByGenreNative(String genre) {
        return movieRepository.findByGenreNative(genre);
    }

    /**
     * Native SQL with Record DTO via {@code @SqlResultSetMapping} : class-based
     * projections require an explicit mapping; interface projections can use column
     * aliases directly.
     *
     * @see #getMovieTitlesByGenreNative(String) the interface-based alternative
     */
    public List<MovieTitleDto> getMovieTitlesByGenreNativeDto(String genre) {
        return movieRepository.findByGenreNativeDto(genre);
    }

    /**
     * Tuple projection : the rawest form. Spring Data uses Tuple internally to back
     * interface projections. Exposing it directly is useful for ad-hoc, dynamic queries
     * where no DTO or interface is worth defining.
     */
    public List<Tuple> getMovieTuplesByGenre(String genre) {
        return movieRepository.findTupleByGenre(genre);
    }

    /**
     * Specifications + projections via fluent API : compose predicates dynamically
     * and project the result with {@code q -> q.as(...)}. Note: the fluent API loads
     * full entities and then maps to the projection; SELECT narrowing does not apply.
     */
    public List<MovieTitleView> getMovieTitlesBySpec(String genre) {
        Specification<Movie> spec = MovieSpecifications.genreEquals(genre);
        return movieRepository.findBy(spec, q -> q.as(MovieTitleView.class).all());
    }

    public List<MovieTitleDto> getMovieDtosBySpec(String genre, int minYear) {
        Specification<Movie> spec = MovieSpecifications.genreEquals(genre)
                .and(MovieSpecifications.releaseYearGreaterOrEqual(minYear));
        return movieRepository.findBy(spec, q -> q.as(MovieTitleDto.class).all());
    }

    /**
     * Hierarchical DTO via service assembly : loads the Movie entity (with lazy
     * actors collection) inside a {@code @Transactional(readOnly = true)} context,
     * then maps to a nested Record DTO structure. For bulk operations, prefer two
     * targeted projection queries to avoid loading full entities.
     *
     * @param movieId the movie ID
     * @return fully populated {@link MovieDetailDto} or {@code null} if not found
     */
    public MovieDetailDto getMovieDetail(Long movieId) {
        Movie movie = movieRepository.findById(movieId).orElse(null);
        if (movie == null) return null;

        List<MovieDetailDto.ActorDto> actorDtos = movie.getActors().stream()
                .map(a -> new MovieDetailDto.ActorDto(a.getId(), a.getFirstName(), a.getLastName()))
                .toList();

        return new MovieDetailDto(
                movie.getId(), movie.getTitle(), movie.getReleaseYear(),
                movie.getGenre(), actorDtos);
    }

    /**
     * Two-query assembly : loads the Actor entity and a separate projection query
     * for movies. This avoids loading the full actors collection on the Movie side
     * and works safely outside a transactional context.
     *
     * @param actorId the actor ID
     * @return populated {@link ActorWithMoviesDto} or {@code null} if not found
     */
    public ActorWithMoviesDto getActorWithMovies(Long actorId) {
        var actor = actorRepository.findById(actorId).orElse(null);
        if (actor == null) return null;

        List<MovieTitleDto> movies = actorRepository.findMoviesByActorId(actorId);
        return new ActorWithMoviesDto(
                actor.getId(), actor.getFirstName(), actor.getLastName(),
                movies);
    }
}
