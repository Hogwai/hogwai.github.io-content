package com.hogwai.jdbcabstractor;

import com.hogwai.jdbcabstractor.dto.Item;
import com.hogwai.jdbcabstractor.dto.ItemCriteria;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class DemoApplication {

    public static void main(String[] args) throws Exception {
        try (HikariDataSource dataSource = createDataSource()) {
            initSchema(dataSource);
            insertData(dataSource);

            ItemService service = new ItemService(dataSource);
            ItemCriteria criteria = new ItemCriteria();
            criteria.setCategoryCode("ELEC");
            criteria.setItemCodes(Arrays.asList("ITEM-001", "ITEM-002", "ITEM-003"));

            System.out.println("=== Pattern 1: Raw JDBC ===");
            printResults(service.getItems(criteria));

            System.out.println("=== Pattern 2: JdbcExecutor with SimpleBinder ===");
            printResults(service.getItemsWithExecutor(criteria));

            System.out.println("=== Pattern 3: Method references ===");
            printResults(service.getItemsCompact(criteria));

            System.out.println("=== Pattern 4: Fluent ParamBinder ===");
            printResults(service.getItemsWithIndexedBinder(criteria));

            System.out.println("=== Pattern 5: RowMapper + DataSource-level ===");
            printResults(service.getItemsWithRowMapper(criteria));
        }
    }

    private static HikariDataSource createDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:jdbcabstractor;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        return new HikariDataSource(config);
    }

    private static void initSchema(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE categories (" +
                "id INT PRIMARY KEY, code VARCHAR(20), label VARCHAR(100))");
            st.execute("CREATE TABLE items (" +
                "id INT PRIMARY KEY, category_id INT, code VARCHAR(20), " +
                "name VARCHAR(100), price DECIMAL(10,2))");
        }
    }

    private static void insertData(DataSource ds) throws Exception {
        try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
            st.execute("INSERT INTO categories VALUES (1, 'ELEC', 'Electronics')");
            st.execute("INSERT INTO categories VALUES (2, 'BOOK', 'Books')");
            st.execute("INSERT INTO items VALUES (1, 1, 'ITEM-001', 'Laptop', 1200.00)");
            st.execute("INSERT INTO items VALUES (2, 1, 'ITEM-002', 'Mouse', 25.00)");
            st.execute("INSERT INTO items VALUES (3, 1, 'ITEM-003', 'Keyboard', 80.00)");
            st.execute("INSERT INTO items VALUES (4, 2, 'ITEM-004', 'Dune', 15.00)");
            st.execute("INSERT INTO items VALUES (5, 2, 'ITEM-005', '1984', 12.00)");
        }
    }

    private static void printResults(List<Item> items) {
        if (items.isEmpty()) {
            System.out.println("  (no results)\n");
            return;
        }
        for (Item i : items) {
            System.out.printf("  %s | %s | $%s | %s%n",
                i.getCode(), i.getName(), i.getPrice(), i.getCategoryLabel());
        }
        System.out.println();
    }
}
