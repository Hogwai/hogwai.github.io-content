package com.hogwai.jpaprojections.repository;

import com.hogwai.jpaprojections.entity.Movie;
import com.hogwai.jpaprojections.projection.GenreStat;
import com.hogwai.jpaprojections.projection.MovieTitleDto;
import com.hogwai.jpaprojections.projection.MovieTitleView;
import com.hogwai.jpaprojections.projection.MovieWithActorsView;
import com.hogwai.jpaprojections.projection.MovieWithLabelView;
import com.hogwai.jpaprojections.projection.MovieTitleGenreDto;
import com.hogwai.jpaprojections.projection.GenreStatDto;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long>,
                                         JpaSpecificationExecutor<Movie> {

    /**
     * Interface Closed Projection: returns only {@code id} and {@code title}.
     * <p>Spring Data optimizes the SQL to {@code SELECT m.id, m.title FROM movie m WHERE m.genre = ?},
     * avoiding a full entity load.
     *
     * @param genre the movie genre filter
     * @return title-only view of matching movies
     */
    List<MovieTitleView> findByGenre(String genre);

    /**
     * Interface Closed Projection with pagination.
     * <p>Spring Data adds {@code LIMIT … OFFSET …} and issues a secondary count query.
     *
     * @param genre    the movie genre filter
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated title-only view
     */
    Page<MovieTitleView> findByGenre(String genre, Pageable pageable);

    /**
     * Interface Closed Projection with a nested {@code Set<ActorView>}.
     * <p>Uses explicit {@code JOIN FETCH} to eagerly load the actors collection in a single query.
     * Without it, each movie would trigger N+1 queries for its actors.
     *
     * @param genre the movie genre filter
     * @return movies with their nested actor views
     */
    @Query("SELECT m FROM Movie m JOIN FETCH m.actors WHERE m.genre = :genre")
    List<MovieWithActorsView> findByGenreWithActors(@Param("genre") String genre);

    /**
     * Nested projection via {@code @EntityGraph} instead of {@code JOIN FETCH}.
     * <p>The {@code Movie.withActors} named entity graph is declared on the {@link Movie} entity.
     * This approach works with derived queries and is reusable across query methods.
     *
     * @param genre the movie genre filter
     * @return movies with their nested actor views
     */
    @EntityGraph("Movie.withActors")
    List<MovieWithActorsView> findByGenreIgnoreCase(String genre);

    /**
     * N+1 DEMONSTRATION: intentionally unsafe, for educational purposes only.
     * <p>Without {@code JOIN FETCH} or {@code @EntityGraph}, each movie's actors collection
     * is lazily loaded one at a time. With 3 Sci-Fi movies this produces 1 + 3 = 4 queries.
     *
     * @param genre the movie genre filter
     * @return movies with actors loaded via N+1
     */
    @Query("SELECT m FROM Movie m WHERE m.genre = :genre")
    List<MovieWithActorsView> findByGenreWithActorsNPlusOne(@Param("genre") String genre);

    /**
     * Record DTO via derived query: Spring Data automatically rewrites the query to
     * {@code SELECT new MovieTitleDto(m.id, m.title) FROM Movie m WHERE m.title LIKE %:title%}.
     * <p>No {@code @Query} annotation needed: the method name drives the query generation
     * and the return type triggers the constructor expression.
     *
     * @param title substring to match against movie titles
     * @return matching movies as {@link MovieTitleDto} records
     */
    List<MovieTitleDto> findByTitleContaining(String title);

    /**
     * Record DTO via explicit {@code @Query} with a JPQL constructor expression.
     * <p>Use this approach when the query cannot be expressed as a derived method name,
     * for example with {@code >=} comparisons or joins.
     *
     * @param year minimum release year (inclusive)
     * @return movies released on or after the given year as {@link MovieTitleDto} records
     */
    @Query("""
            SELECT new com.hogwai.jpaprojections.projection.MovieTitleDto(m.id, m.title)
            FROM Movie m
            WHERE m.releaseYear >= :year
            """)
    List<MovieTitleDto> findMoviesReleasedAfter(@Param("year") int year);

    /**
     * Aggregation with a Record DTO: counts movies per genre using {@code GROUP BY}.
     * <p>The {@link GenreStat} record captures the genre name and its count.
     * Results are ordered by count descending.
     *
     * @return genre statistics ordered by count descending
     */
    @Query("""
            SELECT new com.hogwai.jpaprojections.projection.GenreStat(m.genre, COUNT(m))
            FROM Movie m
            GROUP BY m.genre
            ORDER BY COUNT(m) DESC
            """)
    List<GenreStat> countByGenre();

    /**
     * Native query with interface projection via column aliases.
     * <p>Each column alias must match the Java property name (camelCase).
     * Interface projections work with native queries when aliases are provided.
     *
     * @param genre the movie genre filter
     * @return title-only view of matching movies
     */
    @Query(value = "SELECT m.id AS id, m.title AS title FROM movies m WHERE m.genre = ?1",
           nativeQuery = true)
    List<MovieTitleView> findByGenreNative(String genre);

    /**
     * Native query with Record DTO via {@code @NamedNativeQuery} + {@code @SqlResultSetMapping}.
     * <p>The named native query and result set mapping are declared on the {@link Movie} entity.
     * This is the standard JPA approach for mapping native query results to non-entity classes.
     *
     * @param genre the movie genre filter
     * @return matching movies as {@link MovieTitleDto} records
     */
    @Query(name = "Movie.findByGenreNativeDto", nativeQuery = true)
    List<MovieTitleDto> findByGenreNativeDto(String genre);

    /**
     * Dynamic Projection: the caller selects the return type at runtime.
     * <p>Accepted types include {@link MovieTitleView}, {@link MovieTitleDto}, and
     * {@link Movie}. This is useful when the same query endpoint needs to serve
     * different consumers with different data requirements.
     *
     * @param genre the movie genre filter
     * @param type  the projection class to instantiate for each result
     * @param <T>   the projection type
     * @return movies in the requested projection form
     */
    <T> List<T> findByGenre(String genre, Class<T> type);

    /**
     * {@link Tuple} projection: returns raw columnar access without a pre-defined DTO.
     * <p>Useful for ad-hoc queries where creating a dedicated projection type is overkill.
     * Access values via {@code tuple.get("id", Long.class)} or positional index.
     * <p>Note: Hibernate requires explicit column aliases for Tuple access by name.
     *
     * @param genre the movie genre filter
     * @return tuples with id, title, releaseYear, genre as aliased columns
     */
    @Query("""
            SELECT m.id AS id, m.title AS title,
                   m.releaseYear AS releaseYear, m.genre AS genre
            FROM Movie m WHERE m.genre = :genre
            """)
    List<Tuple> findTupleByGenre(@Param("genre") String genre);

    /**
     * Class-based DTO projection with {@code @PersistenceCreator} for multi-constructor disambiguation.
     * <p>{@link GenreStatDto} is a POJO with two constructors: a no-arg constructor and a
     * parameterised constructor annotated with {@code @PersistenceCreator}. Spring Data uses
     * the annotated constructor to map query results.
     *
     * @return genre statistics ordered by count descending
     */
    @Query("""
            SELECT new com.hogwai.jpaprojections.projection.GenreStatDto(m.genre, COUNT(m))
            FROM Movie m
            GROUP BY m.genre
            ORDER BY COUNT(m) DESC
            """)
    List<GenreStatDto> countByGenreDto();

    /**
     * Multi-select query rewriting demonstration.
     * <p>Spring Data automatically rewrites {@code SELECT m.title, m.genre FROM Movie m ...}
     * to a JPQL constructor expression {@code SELECT new MovieTitleGenreDto(m.title, m.genre) ...}
     * by matching constructor parameter names to the multi-select columns.
     *
     * @param genre the movie genre filter
     * @return title and genre of matching movies as {@link MovieTitleGenreDto} records
     */
    @Query("SELECT m.title, m.genre FROM Movie m WHERE m.genre = :genre")
    List<MovieTitleGenreDto> findTitleAndGenreByGenre(@Param("genre") String genre);

    /**
     * Open projection with {@code @Value} SpEL referencing a Spring bean.
     * <p>The full Movie entity is loaded because open projections disable SELECT optimisation.
     * The {@link MovieWithLabelView#getGenreLabel()} is computed by
     * {@code com.hogwai.jpaprojections.helper.ProjectionHelper#formatGenreLabel(Movie)}
     * via the expression {@code @projectionHelper.formatGenreLabel(target)}.
     *
     * @param genre the movie genre filter
     * @return movies with a computed genre label
     */
    @Query("SELECT m FROM Movie m WHERE m.genre = :genre")
    List<MovieWithLabelView> findWithLabelByGenre(@Param("genre") String genre);
}
