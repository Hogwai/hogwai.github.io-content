package com.hogwai.jdbcabstractor.dto;

import com.hogwai.jdbcabstractor.persistence.RowMapper;

import java.math.BigDecimal;
import java.util.Objects;

public class Item {
    private Integer id;
    private String code;
    private String name;
    private BigDecimal price;
    private String categoryCode;
    private String categoryLabel;

    public static final RowMapper<Item> MAPPER = rs -> {
        Item item = new Item();
        item.setId(rs.getInt("itemId"));
        item.setCode(rs.getString("itemCode"));
        item.setName(rs.getString("itemName"));
        item.setPrice(rs.getBigDecimal("itemPrice"));
        item.setCategoryCode(rs.getString("categoryCode"));
        item.setCategoryLabel(rs.getString("categoryLabel"));
        return item;
    };

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Item)) return false;
        Item item = (Item) o;
        return Objects.equals(id, item.id) &&
                Objects.equals(code, item.code) &&
                Objects.equals(name, item.name) &&
                Objects.equals(price, item.price) &&
                Objects.equals(categoryCode, item.categoryCode) &&
                Objects.equals(categoryLabel, item.categoryLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, code, name, price, categoryCode, categoryLabel);
    }
}
