package com.hogwai.nosql.mongodb.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.hogwai.nosql.mongodb.model.Movie;
import com.hogwai.nosql.mongodb.projection.MovieTitleView;

public interface MovieRepository extends MongoRepository<Movie, String> {

    /**
     * Interface closed projection
     * @param genre genre
     * @return view
     */
    List<MovieTitleView> findByGenre(String genre);

    // @Query with fields attribute: explicit field selection

    /**
     * Query with fields attribute: explicit field selection
     * @param genre genre
     * @return Movie
     */
    @Query(value = "{ 'genre': ?0 }", fields = "{ 'title': 1, 'releaseYear': 1 }")
    List<Movie> findTitleAndYearByGenre(String genre);

    // Aggregation with $project

    /**
     * Aggregation with $project
     * @param genre genre
     * @return view
     */
    @Aggregation(pipeline = {
            "{ $match: { genre: ?0 } }",
            "{ $project: { _id: 0, title: 1, genre: 1 } }"
    })
    List<MovieTitleView> findProjectedByGenre(String genre);

    /**
     * Dynamic projection
     * @param genre genre
     * @param type type
     * @return Typed list
     * @param <T> Type
     */
    <T> List<T> findByGenre(String genre, Class<T> type);

    // Pagination with interface closed projection

    /**
     * Pagination with interface closed projection
     * @param genre genre
     * @param pageable pageable
     * @return Slice of title views
     */
    Slice<MovieTitleView> findSliceByGenre(String genre, Pageable pageable);

    // Pagination with full entity

    /**
     * Pagination with full entity
     * @param genre genre
     * @param pageable pageable
     * @return Page of movies
     */
    Page<Movie> findPageByGenre(String genre, Pageable pageable);
}
