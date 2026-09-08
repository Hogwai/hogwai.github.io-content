package com.hogwai.rowtransformers.transformer;

import com.hogwai.rowtransformers.AbstractPostgresTest;
import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import com.hogwai.rowtransformers.entity.Book;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.hibernate.query.Query;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The same SQL produces 8 different result types depending on the context.
 * The same query goes through ConcreteSqmSelectQueryPlan.determineRowTransformer()
 * and the choice of RowTransformer depends solely on the requested result type.
 */
@DataJpaTest
class TransformerJourneyTest extends AbstractPostgresTest {

    @Autowired
    EntityManager em;

    private static final String HQL = "SELECT b.title, b.pages FROM Book b";

    @Nested
    @DisplayName("SingularReturnImpl: Object[] to row[0]")
    class SingularReturn {

        @Test
        @DisplayName("SELECT b.title returns String (row[0])")
        void singleEntitySelect_returnsFirstElement() {
            List<String> titles = em.createQuery(
                    "SELECT b.title FROM Book b",
                    String.class
            ).getResultList();

            assertThat(titles).hasSize(3);
            assertThat(titles.get(0)).isEqualTo("The Hobbit");
            assertThat(titles.get(1)).isEqualTo("Foundation");
            assertThat(titles.get(2)).isEqualTo("The Lord of the Rings");
        }
    }

    @Nested
    @DisplayName("ConstructorImpl: Object[] to constructor.newInstance(row)")
    class Constructor {

        @Test
        @DisplayName("SELECT new DTO returns BookSummaryDTO")
        void dynamicInstantiation_returnsRecordInstance() {
            List<BookSummaryDTO> dtos = em.createQuery(
                    "SELECT new com.hogwai.rowtransformers.dto.BookSummaryDTO(b.title, b.author.name) FROM Book b",
                    BookSummaryDTO.class
            ).getResultList();

            assertThat(dtos).hasSize(3);
            assertThat(dtos.getFirst()).isInstanceOf(BookSummaryDTO.class);
            assertThat(dtos.getFirst().title()).isEqualTo("The Hobbit");
            assertThat(dtos.getFirst().authorName()).isEqualTo("Tolkien");
        }
    }

    @Nested
    @DisplayName("JpaTupleImpl: access by index and type")
    class JpaTuple {

        @Test
        @DisplayName("Tuple.class provides index and type access")
        void tupleAccess_providesIndexAndTypeAccess() {
            List<Tuple> tuples = em.createQuery(HQL, Tuple.class).getResultList();

            assertThat(tuples).hasSize(3);
            assertThat(tuples.getFirst()).isInstanceOf(Tuple.class);

            assertThat(tuples.getFirst().get(0)).isEqualTo("The Hobbit");
            assertThat(tuples.getFirst().get(1)).isEqualTo(310);

            assertThat(tuples.getFirst().get(0, String.class)).isEqualTo("The Hobbit");
            assertThat(tuples.getFirst().get(1, Integer.class)).isEqualTo(310);

            assertThat(tuples.getFirst().getElements()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("MapImpl: keys are SQL aliases")
    class MapResult {

        @Test
        @DisplayName("Map.class uses HQL aliases as keys")
        @SuppressWarnings("unchecked")
        void mapAccess_providesAliasKeys() {
            List<Map> maps = em.createQuery(
                    "SELECT b.title AS title, b.pages AS pages FROM Book b",
                    Map.class
            ).getResultList();


            assertThat(maps).hasSize(3);
            assertThat(maps.getFirst()).isInstanceOf(Map.class);
            Map<String, Object> typed = (Map<String, Object>) maps.getFirst();
            assertThat(typed)
                    .containsEntry("title", "The Hobbit")
                    .containsEntry("pages", 310);
        }

        @Test
        @DisplayName("Without HQL aliases, keys are index '0', '1'")
        @SuppressWarnings("unchecked")
        void mapWithoutAliases_usesIndexAsKeys() {
            List<Map> maps = em.createQuery(HQL, Map.class).getResultList();

            Map<String, Object> typed = (Map<String, Object>) maps.getFirst();
            assertThat(typed)
                    .containsEntry("0", "The Hobbit")
                    .containsEntry("1", 310);
        }
    }

    @Nested
    @DisplayName("ArrayImpl: identity (returns the same array)")
    class Array {

        @Test
        @DisplayName("Object[].class returns same array as-is")
        void arrayAccess_returnsSameArray() {
            List<Object[]> arrays = em.createQuery(HQL, Object[].class).getResultList();

            assertThat(arrays).hasSize(3);
            assertThat(arrays.getFirst()).isInstanceOf(Object[].class);
            assertThat(arrays.getFirst()[0]).isEqualTo("The Hobbit");
            assertThat(arrays.getFirst()[1]).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("ListImpl: immutable list")
    class ListResult {

        @Test
        @DisplayName("List.class returns immutable list with values")
        void listAccess_returnsImmutableList() {
            List<List> lists = em.createQuery(HQL, List.class).getResultList();

            assertThat(lists).hasSize(3);
            List<?> first = lists.getFirst();
            assertThat(first).isInstanceOf(java.util.List.class);
            assertThat(first.getFirst()).isEqualTo("The Hobbit");
            assertThat(first.get(1)).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("TupleTransformerAdapter: custom lambda")
    class Custom {

        record BookTitle(String title, int pages) {}

        @Test
        @DisplayName("setTupleTransformer returns custom object")
        void customTransformer_createsUserDefinedObject() {
            @SuppressWarnings("unchecked")
            Query<BookTitle> query = (Query<BookTitle>) em.createQuery(HQL);
            List<BookTitle> results = query.setTupleTransformer((tuple, _) ->
                    new BookTitle((String) tuple[0], (Integer) tuple[1])
            ).getResultList();

            assertThat(results).hasSize(3);
            assertThat(results.getFirst()).isInstanceOf(BookTitle.class);
            assertThat(results.getFirst().title()).isEqualTo("The Hobbit");
            assertThat(results.getFirst().pages()).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("StandardImpl: fallback without type specifier")
    class Standard {

        @Test
        @DisplayName("Without type returns Object[]")
        @SuppressWarnings("unchecked")
        void noTypeSpecifier_returnsObjectArray() {
            List<Object[]> results = em.createQuery(HQL).getResultList();

            assertThat(results).hasSize(3);
            assertThat(results.getFirst()).isInstanceOf(Object[].class);
            assertThat(results.getFirst()[0]).isEqualTo("The Hobbit");
            assertThat(results.getFirst()[1]).isEqualTo(310);
        }
    }

    @Nested
    @DisplayName("Bonus: Native SQL, same principle, different path")
    class NativeSql {

        @Test
        @DisplayName("createNativeQuery with TupleTransformer returns same result")
        void nativeQuery_samePrinciple_differentPath() {
            @SuppressWarnings("unchecked")
            List<Book> books = em.createNativeQuery(
                    "SELECT * FROM book",
                    Book.class
            ).getResultList();

            assertThat(books).hasSize(3);
            assertThat(books.getFirst().getTitle()).isEqualTo("The Hobbit");
        }
    }
}
