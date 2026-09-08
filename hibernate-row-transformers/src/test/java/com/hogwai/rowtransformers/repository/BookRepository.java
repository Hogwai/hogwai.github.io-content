package com.hogwai.rowtransformers.repository;

import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import com.hogwai.rowtransformers.entity.Book;
import com.hogwai.rowtransformers.transformer.BookTitleProjection;
import jakarta.persistence.Tuple;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

/**
 * Repository with various query methods.
 * Each method triggers a different underlying RowTransformer.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    // SingularReturnImpl
    List<Book> findByTitle(String title);

    List<Book> findByPagesBetween(int min, int max);

    // SingularReturnImpl (scalar)
    @Query("SELECT b.title FROM Book b")
    List<String> findOnlyTitles();

    // SingularReturnImpl (DTO via SELECT new)
    @Query("SELECT new com.hogwai.rowtransformers.dto.BookSummaryDTO(b.title, b.author.name) FROM Book b")
    List<BookSummaryDTO> findBookSummaries();

    // JpaTupleImpl
    @Query("SELECT b.title, b.pages FROM Book b")
    List<Tuple> findTuples();

    // MapImpl
    @Query("SELECT b.title AS title, b.pages AS pages FROM Book b")
    List<Map<String, Object>> findMaps();

    // ArrayImpl
    @Query("SELECT b.title, b.pages FROM Book b")
    List<Object[]> findArrays();

    // Native SQL (NativeQueryTupleTransformer)
    @Query(value = "SELECT * FROM book", nativeQuery = true)
    List<Book> findAllNative();

    // Interface Projection (SingularReturnImpl underneath)
    @Query("SELECT b.title AS title, b.pages AS pages FROM Book b")
    List<BookTitleProjection> findTitleProjections();
}
