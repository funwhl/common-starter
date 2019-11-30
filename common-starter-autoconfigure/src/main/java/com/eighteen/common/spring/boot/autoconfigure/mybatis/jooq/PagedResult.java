package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:13
 */
public class PagedResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    private int recordCount = 0;
    private List<T> items = null;
    private Map<String, String> options = null;

    private PagedResult() {
    }

    public static <E> PagedResult<E> create(int count, List<E> items) {
        PagedResult<E> result = new PagedResult();
        result.options = new HashMap();
        result.items = items;
        result.recordCount = count;
        return result;
    }

    public Map<String, String> getOptions() {
        return this.options;
    }

    public void addOption(String key, String value) {
        this.options.put(key, value);
    }

    public List<T> getItems() {
        return this.items;
    }

    public int getRecordCount() {
        return this.recordCount;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("Paged Result: ");
        sb.append("    record count:").append(this.recordCount);
        sb.append("    items:").append(this.items);
        sb.append("    options").append(this.options);
        return sb.toString();
    }
}