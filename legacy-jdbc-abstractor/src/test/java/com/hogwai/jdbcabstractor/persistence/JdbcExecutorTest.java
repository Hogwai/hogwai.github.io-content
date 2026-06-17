package com.hogwai.jdbcabstractor.persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.*;

public class JdbcExecutorTest {

    private Connection conn;

    @Before
    public void setUp() throws Exception {
        conn = DriverManager.getConnection("jdbc:h2:mem:jdbctest");
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE test (id INT PRIMARY KEY, name VARCHAR(50))");
            st.execute("INSERT INTO test VALUES (1, 'alpha')");
            st.execute("INSERT INTO test VALUES (2, 'beta')");
        }
    }

    @After
    public void tearDown() throws Exception {
        if (conn != null) conn.close();
    }

    @Test
    public void executeQuery_returnsMappedResults() throws Exception {
        List<String> names = JdbcExecutor.executeQuery(conn,
            "SELECT name FROM test ORDER BY id",
            ps -> {},
            rs -> {
                List<String> results = new java.util.ArrayList<>();
                while (rs.next()) results.add(rs.getString("name"));
                return results;
            }
        );
        assertEquals(Arrays.asList("alpha", "beta"), names);
    }

    @Test
    public void executeQueryWithIndex_bindsAndMaps() throws Exception {
        String result = JdbcExecutor.executeQueryWithIndex(conn,
            "SELECT name FROM test WHERE id = ?",
            binder -> binder.setInt(1),
            rs -> rs.next() ? rs.getString("name") : null
        );
        assertEquals("alpha", result);
    }

    @Test
    public void toList_collectsResults() throws Exception {
        List<String> names = JdbcExecutor.executeQuery(conn,
            "SELECT name FROM test ORDER BY id",
            ps -> {},
            JdbcExecutor.toList(rs -> rs.getString("name"))
        );
        assertEquals(Arrays.asList("alpha", "beta"), names);
    }

    @Test
    public void toOptional_returnsValueWhenPresent() throws Exception {
        Optional<String> result = JdbcExecutor.executeQuery(conn,
            "SELECT name FROM test WHERE id = ?",
            ps -> ps.setInt(1, 1),
            JdbcExecutor.toOptional(rs -> rs.getString("name"))
        );
        assertTrue(result.isPresent());
        assertEquals("alpha", result.get());
    }

    @Test
    public void toOptional_returnsEmptyWhenAbsent() throws Exception {
        Optional<String> result = JdbcExecutor.executeQuery(conn,
            "SELECT name FROM test WHERE id = ?",
            ps -> ps.setInt(1, 999),
            JdbcExecutor.toOptional(rs -> rs.getString("name"))
        );
        assertFalse(result.isPresent());
    }

    @Test
    public void executeUpdate_returnsAffectedRows() throws Exception {
        int count = JdbcExecutor.executeUpdate(conn,
            "UPDATE test SET name = ? WHERE id = ?",
            ps -> {
                ps.setString(1, "gamma");
                ps.setInt(2, 1);
            }
        );
        assertEquals(1, count);
    }
}
