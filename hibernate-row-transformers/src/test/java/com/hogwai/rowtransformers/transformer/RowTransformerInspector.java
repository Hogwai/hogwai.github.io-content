package com.hogwai.rowtransformers.transformer;

import org.hibernate.Session;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.hql.internal.StandardHqlTranslator;
import org.hibernate.query.sqm.internal.ConcreteSqmSelectQueryPlan;
import org.hibernate.query.sqm.tree.SqmStatement;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.sql.results.internal.TupleMetadata;
import org.hibernate.sql.results.spi.RowTransformer;

import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;

/**
 * Test-only helper that calls ConcreteSqmSelectQueryPlan.determineRowTransformer()
 * via reflection to reveal which RowTransformer Hibernate selects for a given query.
 * This shows the real internal decision path without bytecode hacking.
 */
public final class RowTransformerInspector {

    private RowTransformerInspector() {}

    /**
     * Returns the simple class name of the RowTransformer that Hibernate
     * would select for this HQL query and result type.
     */
    public static String inspect(EntityManager em, String hql, Class<?> resultType) {
        return inspect(em, hql, resultType, null);
    }

    /**
     * Overload with explicit TupleMetadata (for Tuple/Map queries).
     */
    public static String inspect(EntityManager em, String hql, Class<?> resultType, TupleMetadata tupleMetadata) {
        Session session = em.unwrap(Session.class);
        try (SessionFactoryImplementor sf = session.getSessionFactory().unwrap(SessionFactoryImplementor.class)){
            // Parse HQL into SqmSelectStatement via the real Hibernate translator
            StandardHqlTranslator translator = (StandardHqlTranslator) sf.getQueryEngine().getHqlTranslator();
            SqmStatement<?> sqm = translator.translate(hql, null);

            if (!(sqm instanceof SqmSelectStatement<?> selectStatement)) {
                throw new IllegalArgumentException("Not a SELECT statement: %s".formatted(hql));
            }

            // Call the protected static determineRowTransformer via reflection
            try {
                Method method = ConcreteSqmSelectQueryPlan.class.getDeclaredMethod(
                        "determineRowTransformer",
                        SqmSelectStatement.class,
                        Class.class,
                        TupleMetadata.class
                );
                method.setAccessible(true);

                RowTransformer<?> transformer = (RowTransformer<?>) method.invoke(
                        null,
                        selectStatement,
                        resultType,
                        tupleMetadata
                );

                return transformer.getClass().getSimpleName();
            } catch (Exception e) {
                throw new RuntimeException("Failed to inspect RowTransformer for: %s".formatted(hql), e);
            }
        }
    }
}
