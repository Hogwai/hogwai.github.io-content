package com.hogwai.jdbcabstractor.dto;

import java.util.List;

public class ItemCriteria {
    private String categoryCode;
    private List<String> itemCodes;

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public List<String> getItemCodes() { return itemCodes; }
    public void setItemCodes(List<String> itemCodes) { this.itemCodes = itemCodes; }
}
