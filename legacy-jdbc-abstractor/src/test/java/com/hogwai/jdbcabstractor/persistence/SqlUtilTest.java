package com.hogwai.jdbcabstractor.persistence;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.*;

public class SqlUtilTest {

    @Test
    public void injectSingleParam_replacesPlaceholder() {
        String result = SqlUtil.injectSingleParam("WHERE code = :code", ":code");
        assertEquals("WHERE code = ?", result);
    }

    @Test
    public void injectInClause_replacesPlaceholderWithQuestionMarks() {
        String result = SqlUtil.injectInClause(
            "WHERE id IN :ids", ":ids", Arrays.asList("a", "b", "c"));
        assertEquals("WHERE id IN (?,?,?)", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void buildInClause_throwsOnEmptyList() {
        SqlUtil.buildInClause(Collections.emptyList());
    }

    @Test
    public void buildInClause_singleElement() {
        assertEquals("(?)", SqlUtil.buildInClause(Arrays.asList(1)));
    }

    @Test
    public void buildInClause_multipleElements() {
        assertEquals("(?,?)", SqlUtil.buildInClause(Arrays.asList(1, 2)));
    }
}
