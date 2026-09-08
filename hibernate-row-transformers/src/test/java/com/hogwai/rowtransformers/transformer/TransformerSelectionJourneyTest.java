package com.hogwai.rowtransformers.transformer;

import com.hogwai.rowtransformers.AbstractPostgresTest;
import com.hogwai.rowtransformers.dto.BookSummaryDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The same SELECT produces 8 different transformers depending on the context.
 * Each test asserts the exact class name of the RowTransformer selected
 * by ConcreteSqmSelectQueryPlan.determineRowTransformer().
 */
@DataJpaTest
class TransformerSelectionJourneyTest extends AbstractPostgresTest {

    @Autowired
    EntityManager em;

    private static final String HQL = "SELECT b.title, b.pages FROM Book b";

    @Test
    void singularReturn_withStringType() {
        String hql = "SELECT b.title FROM Book b";
        String transformer = RowTransformerInspector.inspect(em, hql, String.class);
        assertThat(transformer).isEqualTo("RowTransformerSingularReturnImpl");

        List<String> titles = em.createQuery(hql, String.class).getResultList();
        assertThat(titles.getFirst()).isEqualTo("The Hobbit");
    }

    @Test
    void selectNewDto_returnsSingularReturnNotConstructor() {
        String hql = "SELECT new com.hogwai.rowtransformers.dto.BookSummaryDTO(b.title, b.author.name) FROM Book b";
        String transformer = RowTransformerInspector.inspect(em, hql, BookSummaryDTO.class);
        assertThat(transformer).isEqualTo("RowTransformerSingularReturnImpl");

        List<BookSummaryDTO> dtos = em.createQuery(hql, BookSummaryDTO.class).getResultList();
        assertThat(dtos.getFirst().title()).isEqualTo("The Hobbit");
        assertThat(dtos.getFirst().authorName()).isEqualTo("Tolkien");
    }

    @Test
    void tupleType_returnsJpaTupleImpl() {
        String transformer = RowTransformerInspector.inspect(em, HQL, Tuple.class);
        assertThat(transformer).isEqualTo("RowTransformerJpaTupleImpl");

        List<Tuple> tuples = em.createQuery(HQL, Tuple.class).getResultList();
        assertThat(tuples.getFirst().get(0)).isEqualTo("The Hobbit");
        assertThat(tuples.getFirst().get(1)).isEqualTo(310);
    }

    @Test
    @SuppressWarnings("unchecked")
    void mapType_returnsMapImpl() {
        String transformer = RowTransformerInspector.inspect(em, HQL, Map.class);
        assertThat(transformer).isEqualTo("RowTransformerMapImpl");

        List<Map> maps = em.createQuery(HQL, Map.class).getResultList();
        Map<String, Object> typed = (Map<String, Object>) maps.getFirst();
        assertThat(typed).containsEntry("0", "The Hobbit");
    }

    @Test
    void arrayType_returnsArrayImpl() {
        String transformer = RowTransformerInspector.inspect(em, HQL, Object[].class);
        assertThat(transformer).isEqualTo("RowTransformerArrayImpl");

        List<Object[]> arrays = em.createQuery(HQL, Object[].class).getResultList();
        assertThat(arrays.getFirst()).isInstanceOf(Object[].class);
        assertThat(arrays.getFirst()[0]).isEqualTo("The Hobbit");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listType_returnsListImpl() {
        String transformer = RowTransformerInspector.inspect(em, HQL, List.class);
        assertThat(transformer).isEqualTo("RowTransformerListImpl");

        List<List> lists = em.createQuery(HQL, List.class).getResultList();
        List<String> first = lists.getFirst();
        assertThat(first.getFirst()).isEqualTo("The Hobbit");
        assertThatThrownBy(() -> first.add("should fail"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void customTupleTransformer_returnsAdapter() {
        @SuppressWarnings("unchecked")
        var query = (org.hibernate.query.Query<BookSummaryDTO>) em.createQuery(HQL);
        var results = query.setTupleTransformer((tuple, _) ->
                new BookSummaryDTO((String) tuple[0], "custom")
        ).getResultList();

        assertThat(results.getFirst().authorName()).isEqualTo("custom");
    }

    @Test
    @SuppressWarnings("unchecked")
    void nullResultType_returnsStandardImpl() {
        String transformer = RowTransformerInspector.inspect(em, HQL, null);
        assertThat(transformer).isEqualTo("RowTransformerStandardImpl");

        List<Object[]> results = em.createQuery(HQL).getResultList();
        assertThat(results.getFirst()).isInstanceOf(Object[].class);
    }

    @Test
    void sameHql_differentTypes_differentTransformers() {
        String hql = "SELECT b.title, b.pages FROM Book b";

        assertThat(RowTransformerInspector.inspect(em, hql, null))
                .endsWith("RowTransformerStandardImpl");
        assertThat(RowTransformerInspector.inspect(em, hql, Object[].class))
                .endsWith("RowTransformerArrayImpl");
        assertThat(RowTransformerInspector.inspect(em, hql, List.class))
                .endsWith("RowTransformerListImpl");
        assertThat(RowTransformerInspector.inspect(em, hql, Tuple.class))
                .endsWith("RowTransformerJpaTupleImpl");
        assertThat(RowTransformerInspector.inspect(em, hql, Map.class))
                .endsWith("RowTransformerMapImpl");
    }
}
