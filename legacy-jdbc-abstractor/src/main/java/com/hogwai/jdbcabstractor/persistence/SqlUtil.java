package com.hogwai.jdbcabstractor.persistence;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

public final class SqlUtil {

    private SqlUtil() {}

    public static String injectSingleParam(String query, String placeholder) {
        return query.replace(placeholder, "?");
    }

    public static String injectInClause(String query, String placeholder, List<?> values) {
        return query.replace(placeholder, buildInClause(values));
    }

    public static String buildInClause(List<?> values) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty for IN clause");
        }
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < values.size(); i++) {
            sb.append("?");
            if (i < values.size() - 1) sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    public static int bindListAsString(PreparedStatement ps, int startIndex, List<?> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            ps.setString(startIndex + i, String.valueOf(values.get(i)));
        }
        return startIndex + values.size();
    }
}
