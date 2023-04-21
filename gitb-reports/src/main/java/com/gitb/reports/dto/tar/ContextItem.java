package com.gitb.reports.dto.tar;

import java.util.List;

public class ContextItem {

    private final String key;
    private final String value;
    private final List<ContextItem> items;

    public ContextItem(String key, String value) {
        this(key, value, null);
    }

    public ContextItem(String key, List<ContextItem> items) {
        this(key, null, items);
    }

    private ContextItem(String key, String value, List<ContextItem> items) {
        this.key = key;
        this.value = value;
        this.items = items;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public List<ContextItem> getItems() {
        return items;
    }

}
