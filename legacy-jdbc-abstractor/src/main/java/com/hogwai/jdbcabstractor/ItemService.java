package com.hogwai.jdbcabstractor;

import com.hogwai.jdbcabstractor.dto.Item;
import com.hogwai.jdbcabstractor.dto.ItemCriteria;
import com.hogwai.jdbcabstractor.persistence.JdbcExecutor;
import com.hogwai.jdbcabstractor.persistence.JdbcExecutor.IndexedBinder;
import com.hogwai.jdbcabstractor.persistence.SqlUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

public class ItemService {

    private final DataSource dataSource;

    private static final String GET_ITEMS_BY_CATEGORY_AND_CODES =
        "SELECT c.code AS categoryCode, c.label AS categoryLabel, " +
        "       i.id AS itemId, i.code AS itemCode, i.name AS itemName, i.price AS itemPrice " +
        "FROM categories c " +
        "INNER JOIN items i ON i.category_id = c.id " +
        "WHERE c.code = :categoryCode " +
        "  AND i.code IN :itemCodes " +
        "ORDER BY i.name";

    public ItemService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Item> getItems(ItemCriteria criteria) {
        if (criteria == null || isEmpty(criteria.getCategoryCode()) || isEmpty(criteria.getItemCodes())) {
            return Collections.emptyList();
        }
        String sql = SqlUtil.injectSingleParam(GET_ITEMS_BY_CATEGORY_AND_CODES, ":categoryCode");
        sql = SqlUtil.injectInClause(sql, ":itemCodes", criteria.getItemCodes());

        List<Item> results = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setString(idx++, criteria.getCategoryCode());
            SqlUtil.bindListAsString(ps, idx, criteria.getItemCodes());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
        return results;
    }

    public List<Item> getItemsWithExecutor(ItemCriteria criteria) {
        if (criteria == null || isEmpty(criteria.getCategoryCode()) || isEmpty(criteria.getItemCodes())) {
            return Collections.emptyList();
        }
        String sql = SqlUtil.injectSingleParam(GET_ITEMS_BY_CATEGORY_AND_CODES, ":categoryCode");
        sql = SqlUtil.injectInClause(sql, ":itemCodes", criteria.getItemCodes());

        try (Connection conn = dataSource.getConnection()) {
            return JdbcExecutor.executeQuery(conn, sql,
                ps -> {
                    ps.setString(1, criteria.getCategoryCode());
                    SqlUtil.bindListAsString(ps, 2, criteria.getItemCodes());
                },
                rs -> {
                    List<Item> results = new ArrayList<>();
                    while (rs.next()) {
                        results.add(mapItem(rs));
                    }
                    return results;
                }
            );
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    public List<Item> getItemsCompact(ItemCriteria criteria) {
        if (criteria == null || isEmpty(criteria.getCategoryCode()) || isEmpty(criteria.getItemCodes())) {
            return Collections.emptyList();
        }
        String sql = SqlUtil.injectSingleParam(GET_ITEMS_BY_CATEGORY_AND_CODES, ":categoryCode");
        sql = SqlUtil.injectInClause(sql, ":itemCodes", criteria.getItemCodes());

        try (Connection conn = dataSource.getConnection()) {
            return JdbcExecutor.executeQuery(conn, sql,
                ps -> bindParams(criteria, ps),
                ItemService::mapResults
            );
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    public List<Item> getItemsWithIndexedBinder(ItemCriteria criteria) {
        if (criteria == null || isEmpty(criteria.getCategoryCode()) || isEmpty(criteria.getItemCodes())) {
            return Collections.emptyList();
        }
        String sql = SqlUtil.injectSingleParam(GET_ITEMS_BY_CATEGORY_AND_CODES, ":categoryCode");
        sql = SqlUtil.injectInClause(sql, ":itemCodes", criteria.getItemCodes());

        try (Connection conn = dataSource.getConnection()) {
            return JdbcExecutor.executeQueryWithIndex(conn, sql,
                binder -> binder
                    .setString(criteria.getCategoryCode())
                    .setList(criteria.getItemCodes()),
                ItemService::mapResults
            );
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    public List<Item> getItemsWithRowMapper(ItemCriteria criteria) {
        if (criteria == null || isEmpty(criteria.getCategoryCode()) || isEmpty(criteria.getItemCodes())) {
            return Collections.emptyList();
        }
        String sql = SqlUtil.injectSingleParam(GET_ITEMS_BY_CATEGORY_AND_CODES, ":categoryCode");
        sql = SqlUtil.injectInClause(sql, ":itemCodes", criteria.getItemCodes());

        try {
            IndexedBinder indexedBind = binder -> binder
                .setString(criteria.getCategoryCode())
                .setList(criteria.getItemCodes());
            return JdbcExecutor.executeQuery(dataSource, sql,
                indexedBind,
                JdbcExecutor.toList(Item.MAPPER)
            );
        } catch (SQLException e) {
            throw new RuntimeException("Query failed", e);
        }
    }

    private static Item mapItem(ResultSet rs) throws SQLException {
        return Item.MAPPER.map(rs);
    }

    private static List<Item> mapResults(ResultSet rs) throws SQLException {
        List<Item> results = new ArrayList<>();
        while (rs.next()) {
            results.add(mapItem(rs));
        }
        return results;
    }

    private static void bindParams(ItemCriteria criteria, PreparedStatement ps) throws SQLException {
        int idx = 1;
        ps.setString(idx++, criteria.getCategoryCode());
        SqlUtil.bindListAsString(ps, idx, criteria.getItemCodes());
    }

    private static boolean isEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }
}
