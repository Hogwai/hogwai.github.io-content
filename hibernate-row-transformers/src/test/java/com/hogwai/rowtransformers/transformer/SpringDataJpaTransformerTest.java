package com.hogwai.rowtransformers.transformer;

import com.hogwai.rowtransformers.AbstractPostgresTest;
import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import com.hogwai.rowtransformers.entity.Book;
import com.hogwai.rowtransformers.repository.BookRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Spring Data JPA uses the same Hibernate pipeline underneath.
 * Each repository method triggers a specific RowTransformer.
 */
@DataJpaTest
class SpringDataJpaTransformerTest extends AbstractPostgresTest {

    @Autowired
    BookRepository repository;

    @Autowired
    EntityManager em;

    @Nested
    @DisplayName("SingularReturnImpl: generated findBy*() methods")
    class SingularReturn {

        @Test
        @DisplayName("findByTitle() returns SingularReturnImpl (entity)")
        void findByTitle_usesSingularReturn() {
            List<Book> books = repository.findByTitle("The Hobbit");

            assertThat(books).hasSize(1);
            assertThat(books.getFirst()).isInstanceOf(Book.class);
            assertThat(books.getFirst().getTitle()).isEqualTo("The Hobbit");
        }

        @Test
        @DisplayName("findByPagesBetween() returns SingularReturnImpl (entity)")
        void findByPagesBetween_usesSingularReturn() {
            List<Book> books = repository.findByPagesBetween(200, 400);

            assertThat(books).hasSize(2);
        }

        @Test
        @DisplayName("findOnlyTitles() returns SingularReturnImpl (scalar)")
        void findOnlyTitles_usesSingularReturn() {
            List<String> titles = repository.findOnlyTitles();

            assertThat(titles).hasSize(3);
            assertThat(titles.getFirst()).isEqualTo("The Hobbit");
        }
    }

    @Nested
    @DisplayName("SingularReturnImpl: SELECT new DTO via @Query")
    class ConstructorViaQuery {

        @Test
        @DisplayName("@Query with SELECT new DTO returns SingularReturnImpl")
        void selectNewDto_usesSingularReturn() {
            List<BookSummaryDTO> summaries = repository.findBookSummaries();

            assertThat(summaries).hasSize(3);
            assertThat(summaries.getFirst()).isInstanceOf(BookSummaryDTO.class);
            assertThat(summaries.getFirst().title()).isEqualTo("The Hobbit");
            assertThat(summaries.getFirst().authorName()).isEqualTo("Tolkien");
        }
    }

    @Nested
    @DisplayName("JpaTupleImpl: @Query returns Tuple")
    class TupleResult {

        @Test
        @DisplayName("findTuples() returns JpaTupleImpl")
        void findTuples_usesJpaTuple() {
            List<Tuple> tuples = repository.findTuples();

            assertThat(tuples).hasSize(3);
            assertThat(tuples.getFirst().get(0)).isEqualTo("The Hobbit");
            assertThat(tuples.getFirst().get(1)).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("MapImpl: @Query returns Map")
    class MapResult {

        @Test
        @DisplayName("findMaps() returns MapImpl")
        void findMaps_usesMapImpl() {
            List<Map<String, Object>> maps = repository.findMaps();

            assertThat(maps).hasSize(3);
            assertThat(maps.getFirst()).containsEntry("title", "The Hobbit");
            assertThat(maps.getFirst()).containsEntry("pages", 310);
        }
    }

    @Nested
    @DisplayName("ArrayImpl: @Query returns Object[]")
    class Array {

        @Test
        @DisplayName("findArrays() returns ArrayImpl")
        void findArrays_usesArrayImpl() {
            List<Object[]> arrays = repository.findArrays();

            assertThat(arrays).hasSize(3);
            assertThat(arrays.getFirst()).isInstanceOf(Object[].class);
            assertThat(arrays.getFirst()[0]).isEqualTo("The Hobbit");
            assertThat(arrays.getFirst()[1]).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("Native SQL: @Query(nativeQuery = true)")
    class NativeSql {

        @Test
        @DisplayName("findAllNative() returns NativeQueryTupleTransformer")
        void findAllNative_usesNativeQuery() {
            List<Book> books = repository.findAllNative();

            assertThat(books).hasSize(3);
            assertThat(books.getFirst()).isInstanceOf(Book.class);
            assertThat(books.getFirst().getTitle()).isEqualTo("The Hobbit");
        }
    }

    @Nested
    @DisplayName("Interface Projection: Spring Data proxy")
    class Projection {

        @Test
        @DisplayName("findTitleProjections() returns interface proxy")
        void findProjections_usesInterfaceProxy() {
            List<BookTitleProjection> projections = repository.findTitleProjections();

            assertThat(projections).hasSize(3);
            assertThat(projections.getFirst()).isInstanceOf(BookTitleProjection.class);
            assertThat(projections.getFirst().getTitle()).isEqualTo("The Hobbit");
            assertThat(projections.getFirst().getPages()).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("Bonus: same result, 5 different transformers via Spring Data")
    class DecisionTree {

        @Test
        @DisplayName("Repository methods trigger different transformers")
        void repositoryMethods_triggerDifferentTransformers() {
            List<Book> books = repository.findByTitle("The Hobbit");
            assertThat(books.getFirst()).isInstanceOf(Book.class);

            List<String> titles = repository.findOnlyTitles();
            assertThat(titles).contains("The Hobbit");

            List<Tuple> tuples = repository.findTuples();
            assertThat(tuples.getFirst().get(0)).isEqualTo("The Hobbit");

            List<Map<String, Object>> maps = repository.findMaps();
            assertThat(maps.getFirst()).containsEntry("title", "The Hobbit");

            List<Object[]> arrays = repository.findArrays();
            assertThat(arrays.getFirst()[0]).isEqualTo("The Hobbit");
        }
    }
}
