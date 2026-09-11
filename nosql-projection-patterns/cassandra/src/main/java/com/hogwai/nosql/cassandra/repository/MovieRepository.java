package com.hogwai.nosql.cassandra.repository;

import java.util.List;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;
import com.hogwai.nosql.cassandra.model.Movie;
import com.hogwai.nosql.cassandra.projection.GenreStat;

public interface MovieRepository extends CrudRepository<Movie, String> {

    /**
     * Explicit CQL SELECT:
     * @param genre genre
     * @return Movies
     */
    @Query("SELECT id, title, genre FROM movies WHERE genre = ?0 ALLOW FILTERING")
    List<Movie> findByGenreProjected(String genre);

    /**
     * Full entity query (for benchmark comparison)
     * @param genre genre
     * @return Movies
     */
    @Query("SELECT * FROM movies WHERE genre = ?0 ALLOW FILTERING")
    List<Movie> findByGenreFull(String genre);

    /**
     * Search with title containment: only title + releaseYear (matching MongoDB @Query fields)
     * @param title title
     * @return Movies
     */
    @Query("SELECT id, title, release_year FROM movies WHERE title CONTAINS ?0 ALLOW FILTERING")
    List<Movie> searchByTitle(String title);

    /**
     * Aggregation: count movies per genre (Cassandra equivalent of MongoDB $project)
     * @param genre genre
     * @return count
     */
    @Query("SELECT genre, count(*) AS movieCount FROM movies WHERE genre = ?0 GROUP BY genre ALLOW FILTERING")
    List<GenreStat> countByGenre(String genre);

    /**
     * Pagination with explicit column projection
     * @param genre genre
     * @param pageable pageable
     * @return Slice of Movies
     */
    @Query("SELECT id, title, genre FROM movies WHERE genre = ?0 ALLOW FILTERING")
    Slice<Movie> findSliceByGenreProjected(String genre, Pageable pageable);
}
