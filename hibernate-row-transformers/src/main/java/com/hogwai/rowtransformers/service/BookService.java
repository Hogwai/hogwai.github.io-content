package com.hogwai.rowtransformers.service;

import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import com.hogwai.rowtransformers.entity.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.hibernate.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Service layer that demonstrates different RowTransformer selections.
 * Each method uses a different query style to trigger a specific transformer.
 */
@Service
public class BookService {

    public static final String SELECT_TITLE_PAGES_FROM_BOOK = "SELECT b.title, b.pages FROM Book b";
    private final EntityManager em;

    public BookService(EntityManager em) {
        this.em = em;
    }

    /**
     * SingularReturnImpl: simple entity query.
     */
    public List<Book> findAll() {
        return em.createQuery("SELECT b FROM Book b", Book.class).getResultList();
    }

    /**
     * SingularReturnImpl: SELECT new DTO (constructor called at SQL AST level).
     */
    public List<BookSummaryDTO> findSummaries() {
        return em.createQuery(
                "SELECT new com.hogwai.rowtransformers.dto.BookSummaryDTO(b.title, b.author.name) FROM Book b",
                BookSummaryDTO.class
        ).getResultList();
    }

    /**
     * JpaTupleImpl: Tuple result type.
     */
    public List<Tuple> findTuples() {
        return em.createQuery(SELECT_TITLE_PAGES_FROM_BOOK, Tuple.class).getResultList();
    }

    /**
     * MapImpl: Map result type with aliases.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> findMaps() {
        return (List<Map<String, Object>>) (List<?>) em.createQuery(
                "SELECT b.title AS title, b.pages AS pages FROM Book b", Map.class
        ).getResultList();
    }

    /**
     * ArrayImpl: Object[] result type.
     */
    public List<Object[]> findArrays() {
        return em.createQuery(SELECT_TITLE_PAGES_FROM_BOOK, Object[].class).getResultList();
    }

    /**
     * ListImpl: List result type (immutable).
     */
    @SuppressWarnings("unchecked")
    public List<List<Object>> findLists() {
        return (List<List<Object>>) (List<?>) em.createQuery(
                SELECT_TITLE_PAGES_FROM_BOOK, List.class
        ).getResultList();
    }

    /**
     * TupleTransformerAdapter: custom lambda via setTupleTransformer.
     */
    public List<BookSummaryDTO> findCustom() {
        @SuppressWarnings("unchecked")
        Query<BookSummaryDTO> query = (Query<BookSummaryDTO>) em.createQuery(
                "SELECT b.title, b.author.name FROM Book b"
        );
        return query.setTupleTransformer((tuple, aliases) ->
                new BookSummaryDTO((String) tuple[0], (String) tuple[1])
        ).getResultList();
    }

    /**
     * Native SQL: native query with result class.
     */
    @SuppressWarnings("unchecked")
    public List<Book> findAllNative() {
        return em.createNativeQuery("SELECT * FROM book", Book.class).getResultList();
    }
}
