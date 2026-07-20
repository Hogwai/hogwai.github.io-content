package com.hogwai.jpaprojections.controller;

import com.hogwai.jpaprojections.projection.*;
import com.hogwai.jpaprojections.service.ProjectionsDemoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing one endpoint per projection type.
 * <p>Each method maps directly to a projection technique demonstrated by this project.
 */
@RestController
@RequestMapping("/api")
public class ProjectionsDemoController {

    private final ProjectionsDemoService service;

    public ProjectionsDemoController(ProjectionsDemoService service) {
        this.service = service;
    }

    /**
     * 1. Interface Closed Projection.
     * <p>Spring Data optimises the SELECT to only the columns needed by {@link MovieTitleView}.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return title-only view of matching movies
     */
    @GetMapping("/movies")
    public List<MovieTitleView> getMoviesByGenre(@RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMovieTitlesByGenre(genre);
    }

    /**
     * 1b. Interface Closed Projection with pagination.
     * <p>Supports {@code page}, {@code size}, and {@code sort} query parameters.
     *
     * @param genre    movie genre filter (default: Sci-Fi)
     * @param pageable pagination parameters injected by Spring
     * @return paginated title-only view
     */
    @GetMapping("/movies/paged")
    public Page<MovieTitleView> getMoviesPaged(
            @RequestParam(defaultValue = "Sci-Fi") String genre,
            Pageable pageable) {
        return service.getMovieTitlesByGenrePaged(genre, pageable);
    }

    /**
     * 2. Interface Closed Projection with nested actors (JOIN FETCH).
     * <p>Uses an explicit {@code @Query} with {@code JOIN FETCH} to load actors in the
     * same round-trip, avoiding N+1 on the collection.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return movies with nested actor views
     */
    @GetMapping("/movies/with-actors")
    public List<MovieWithActorsView> getMoviesWithActors(@RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMoviesWithActorsByGenre(genre);
    }

    /**
     * 2b. Interface + {@code @EntityGraph} (alternative to JOIN FETCH).
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return movies with nested actor views
     */
    @GetMapping("/movies/with-actors/entity-graph")
    public List<MovieWithActorsView> getMoviesWithActorsEntityGraph(
            @RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMoviesWithActorsByGenreEntityGraph(genre);
    }

    /**
     * 2c. N+1 demonstration: intentionally unsafe.
     * <p>Accessing the actors collection on each movie triggers separate lazy-load queries.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return movies with actors loaded via N+1 queries
     */
    @GetMapping("/movies/with-actors/n-plus-one")
    public List<MovieWithActorsView> getMoviesWithActorsNPlusOne(
            @RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMoviesWithActorsNPlusOne(genre);
    }

    /**
     * 3. Interface + default method (instead of {@code @Value} SpEL).
     * <p>The interface stays <em>closed</em> so Spring Data still optimises the SELECT.
     *
     * @param lastName actor last name filter (default: Freeman)
     * @return actors with their computed full name
     */
    @GetMapping("/actors")
    public List<ActorNameView> getActorsByLastName(@RequestParam(defaultValue = "Freeman") String lastName) {
        return service.getActorsByLastName(lastName);
    }

    /**
     * 4. Record DTO via derived query.
     * <p>No {@code @Query} needed: Spring Data rewrites to a constructor expression.
     *
     * @param title substring to match (default: The)
     * @return matching movies as {@link MovieTitleDto} records
     */
    @GetMapping("/movies/search")
    public List<MovieTitleDto> searchMovies(@RequestParam(defaultValue = "The") String title) {
        return service.searchMoviesByTitle(title);
    }

    /**
     * 5. Record DTO via explicit {@code @Query} constructor expression.
     * <p>Required when the query uses operators like {@code >=} that cannot be derived
     * from the method name alone.
     *
     * @param year minimum release year (default: 2000)
     * @return movies released on or after the given year
     */
    @GetMapping("/movies/after")
    public List<MovieTitleDto> getMoviesAfter(@RequestParam(defaultValue = "2000") int year) {
        return service.getMoviesReleasedAfter(year);
    }

    /**
     * 6. Aggregation with a Record: {@code GROUP BY} result mapped to {@link GenreStat}.
     *
     * @return genre statistics ordered by count descending
     */
    @GetMapping("/movies/stats/genre")
    public List<GenreStat> getGenreStats() {
        return service.getGenreStatistics();
    }

    /**
     * 7. Dynamic Projection: the caller selects the return type via query parameter.
     * <p>Use {@code ?projection=full} for full entities, {@code ?projection=dto} for
     * {@link MovieTitleDto}, or omit for {@link MovieTitleView}.
     *
     * @param genre      movie genre filter (default: Sci-Fi)
     * @param projection the projection mode (interface / dto / full, default: interface)
     * @return movies in the requested projection
     */
    @GetMapping("/movies/dynamic")
    public List<?> getMoviesDynamic(
            @RequestParam(defaultValue = "Sci-Fi") String genre,
            @RequestParam(defaultValue = "interface") String projection) {

        Class<?> type = switch (projection) {
            case "full" -> com.hogwai.jpaprojections.entity.Movie.class;
            case "dto" -> MovieTitleDto.class;
            default -> MovieTitleView.class;
        };
        return service.getMoviesByGenre(genre, type);
    }

    /**
     * 8. Native query + interface projection via column aliases.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return title-only view of matching movies
     */
    @GetMapping("/movies/native")
    public List<MovieTitleView> getMoviesNative(@RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMovieTitlesByGenreNative(genre);
    }

    /**
     * 9. Native query + Record DTO via {@code @SqlResultSetMapping}.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return matching movies as {@link MovieTitleDto} records
     */
    @GetMapping("/movies/native/dto")
    public List<MovieTitleDto> getMoviesNativeDto(@RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMovieTitlesByGenreNativeDto(genre);
    }

    /**
     * 10. Tuple projection: raw columnar access.
     *
     * @param genre movie genre filter (default: Sci-Fi)
     * @return list of tuples with id, title, releaseYear, genre as keys
     */
    @GetMapping("/movies/tuples")
    public List<Map<String, Object>> getMovieTuples(@RequestParam(defaultValue = "Sci-Fi") String genre) {
        return service.getMovieTuplesByGenre(genre).stream()
                .map(t -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", t.get("id", Long.class));
                    map.put("title", t.get("title", String.class));
                    map.put("releaseYear", t.get("releaseYear", Integer.class));
                    map.put("genre", t.get("genre", String.class));
                    return map;
                })
                .toList();
    }

    /**
     * 11. Specifications + projections (fluent API).
     * <p>Composes dynamic predicates with projection via the {@code findBy} fluent API.
     *
     * @param genre   movie genre filter (default: Sci-Fi)
     * @param minYear minimum release year (optional)
     * @return movies in the requested projection
     */
    @GetMapping("/movies/spec")
    public List<MovieTitleDto> getMoviesBySpec(
            @RequestParam(defaultValue = "Sci-Fi") String genre,
            @RequestParam(defaultValue = "0") int minYear) {
        return service.getMovieDtosBySpec(genre, minYear);
    }

    /**
     * 12. Hierarchical DTO assembled from the entity graph in the service layer.
     * <p>The movie entity is loaded with its actors (same transaction), then mapped
     * into a structured {@link MovieDetailDto}.
     *
     * @param id movie identifier
     * @return detailed movie DTO with actors, or 404 if not found
     */
    @GetMapping("/movies/{id}/detail")
    public ResponseEntity<MovieDetailDto> getMovieDetail(@PathVariable Long id) {
        MovieDetailDto dto = service.getMovieDetail(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }

    /**
     * 13. Actor-with-movies DTO assembled via two independent queries.
     * <p>Loads the actor entity and separately fetches their movies.
     *
     * @param id actor identifier
     * @return actor DTO with their movie list, or 404 if not found
     */
    @GetMapping("/actors/{id}/movies")
    public ResponseEntity<ActorWithMoviesDto> getActorWithMovies(@PathVariable Long id) {
        ActorWithMoviesDto dto = service.getActorWithMovies(id);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.notFound().build();
    }
}
