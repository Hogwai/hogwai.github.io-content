package com.hogwai.jdbcabstractor.persistence;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcExecutor {

    @FunctionalInterface
    public interface SimpleBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    @FunctionalInterface
    public interface IndexedBinder {
        void bind(ParamBinder binder) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultProcessor<T> {
        T process(ResultSet rs) throws SQLException;
    }

    public static class ParamBinder {
        private final PreparedStatement stmt;
        private int index = 1;

        public ParamBinder(PreparedStatement stmt) {
            this.stmt = stmt;
        }

        public ParamBinder set(Object value) throws SQLException {
            stmt.setObject(index++, value);
            return this;
        }

        public ParamBinder setInt(int value) throws SQLException {
            stmt.setInt(index++, value);
            return this;
        }

        public ParamBinder setString(String value) throws SQLException {
            stmt.setString(index++, value);
            return this;
        }

        public ParamBinder setLong(long value) throws SQLException {
            stmt.setLong(index++, value);
            return this;
        }

        public ParamBinder setDouble(double value) throws SQLException {
            stmt.setDouble(index++, value);
            return this;
        }

        public ParamBinder setBigDecimal(BigDecimal value) throws SQLException {
            stmt.setBigDecimal(index++, value);
            return this;
        }

        public ParamBinder setBoolean(boolean value) throws SQLException {
            stmt.setBoolean(index++, value);
            return this;
        }

        public ParamBinder setDate(java.util.Date value) throws SQLException {
            stmt.setDate(index++, new java.sql.Date(value.getTime()));
            return this;
        }

        public ParamBinder setTimestamp(java.util.Date value) throws SQLException {
            stmt.setTimestamp(index++, new Timestamp(value.getTime()));
            return this;
        }

        public ParamBinder setList(Iterable<?> values) throws SQLException {
            for (Object val : values) {
                set(val);
            }
            return this;
        }
    }

    public static <T> ResultProcessor<List<T>> toList(RowMapper<T> mapper) {
        return rs -> {
            List<T> results = new ArrayList<>();
            while (rs.next()) {
                results.add(mapper.map(rs));
            }
            return results;
        };
    }

    public static <T> ResultProcessor<Optional<T>> toOptional(RowMapper<T> mapper) {
        return rs -> {
            if (rs.next()) {
                return Optional.ofNullable(mapper.map(rs));
            }
            return Optional.empty();
        };
    }

    public static <T> T executeQuery(DataSource ds, String query,
                                     IndexedBinder binder, ResultProcessor<T> processor) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return executeQueryWithIndex(conn, query, binder, processor);
        }
    }

    public static <T> T executeQuery(DataSource ds, String query,
                                     SimpleBinder binder, ResultProcessor<T> processor) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return executeQuery(conn, query, binder, processor);
        }
    }

    public static int executeUpdate(DataSource ds, String query,
                                    SimpleBinder binder) throws SQLException {
        try (Connection conn = ds.getConnection()) {
            return executeUpdate(conn, query, binder);
        }
    }

    public static <T> T executeQueryWithIndex(Connection conn, String query,
                                              IndexedBinder binder, ResultProcessor<T> processor) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            binder.bind(new ParamBinder(stmt));
            try (ResultSet rs = stmt.executeQuery()) {
                return processor.process(rs);
            }
        }
    }

    public static <T> T executeQuery(Connection conn, String query,
                                     SimpleBinder binder, ResultProcessor<T> processor) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return processor.process(rs);
            }
        }
    }

    public static int executeUpdate(Connection conn, String query,
                                    SimpleBinder binder) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            binder.bind(ps);
            return ps.executeUpdate();
        }
    }
}
