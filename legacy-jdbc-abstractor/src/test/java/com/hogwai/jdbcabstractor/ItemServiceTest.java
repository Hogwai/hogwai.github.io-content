package com.hogwai.jdbcabstractor;

import com.hogwai.jdbcabstractor.dto.Item;
import com.hogwai.jdbcabstractor.dto.ItemCriteria;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class ItemServiceTest {

    private static DataSource dataSource;
    private static ItemService service;

    @BeforeClass
    public static void setUp() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:svctest;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        dataSource = new HikariDataSource(config);

        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE categories (id INT PRIMARY KEY, code VARCHAR(20), label VARCHAR(100))");
            st.execute("CREATE TABLE items (id INT PRIMARY KEY, category_id INT, code VARCHAR(20), name VARCHAR(100), price DECIMAL(10,2))");
            st.execute("INSERT INTO categories VALUES (1, 'ELEC', 'Electronics')");
            st.execute("INSERT INTO categories VALUES (2, 'BOOK', 'Books')");
            st.execute("INSERT INTO items VALUES (1, 1, 'ITEM-001', 'Laptop', 1200.00)");
            st.execute("INSERT INTO items VALUES (2, 1, 'ITEM-002', 'Mouse', 25.00)");
            st.execute("INSERT INTO items VALUES (3, 1, 'ITEM-003', 'Keyboard', 80.00)");
        }
        service = new ItemService(dataSource);
    }

    @AfterClass
    public static void tearDown() {
        if (dataSource instanceof HikariDataSource) {
            ((HikariDataSource) dataSource).close();
        }
    }

    private ItemCriteria elecCriteria() {
        ItemCriteria c = new ItemCriteria();
        c.setCategoryCode("ELEC");
        c.setItemCodes(Arrays.asList("ITEM-001", "ITEM-002", "ITEM-003"));
        return c;
    }

    @Test
    public void allPatternsProduceSameResults() {
        ItemCriteria criteria = elecCriteria();
        List<Item> r1 = service.getItems(criteria);
        List<Item> r2 = service.getItemsWithExecutor(criteria);
        List<Item> r3 = service.getItemsCompact(criteria);
        List<Item> r4 = service.getItemsWithIndexedBinder(criteria);
        List<Item> r5 = service.getItemsWithRowMapper(criteria);

        assertEquals(3, r1.size());
        assertEquals(r1, r2);
        assertEquals(r1, r3);
        assertEquals(r1, r4);
        assertEquals(r1, r5);
    }

    @Test
    public void returnsEmptyForNullCriteria() {
        assertTrue(service.getItems(null).isEmpty());
        assertTrue(service.getItemsWithExecutor(null).isEmpty());
        assertTrue(service.getItemsCompact(null).isEmpty());
        assertTrue(service.getItemsWithIndexedBinder(null).isEmpty());
        assertTrue(service.getItemsWithRowMapper(null).isEmpty());
    }

    @Test
    public void returnsEmptyForEmptyItemCodes() {
        ItemCriteria c = new ItemCriteria();
        c.setCategoryCode("ELEC");
        c.setItemCodes(Collections.emptyList());
        assertTrue(service.getItems(c).isEmpty());
        assertTrue(service.getItemsWithExecutor(c).isEmpty());
        assertTrue(service.getItemsCompact(c).isEmpty());
        assertTrue(service.getItemsWithIndexedBinder(c).isEmpty());
        assertTrue(service.getItemsWithRowMapper(c).isEmpty());
    }

    @Test
    public void returnsEmptyForUnknownCategory() {
        ItemCriteria c = new ItemCriteria();
        c.setCategoryCode("UNKNOWN");
        c.setItemCodes(Arrays.asList("ITEM-001"));
        assertTrue(service.getItems(c).isEmpty());
        assertTrue(service.getItemsWithExecutor(c).isEmpty());
        assertTrue(service.getItemsCompact(c).isEmpty());
        assertTrue(service.getItemsWithIndexedBinder(c).isEmpty());
        assertTrue(service.getItemsWithRowMapper(c).isEmpty());
    }
}
