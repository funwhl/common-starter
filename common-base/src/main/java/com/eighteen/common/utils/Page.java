package com.eighteen.common.utils;

import lombok.Data;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by wangwei. Date: 2019/9/22 Time: 10:38
 */
@Data
public class Page<T> {
    public static final int DEFAULT_PAGE_SIZE = 50;
    public static final int DEFAULT_PAGE_NO = 1;

    private int pageNo = DEFAULT_PAGE_NO;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int count = 0;
    private int totalPage = 0;
    private List<T> results = new ArrayList<>();
    private Map<String, Object> params = new HashMap<>();
//    private Cache<String,String> cache = CacheBuilder.newBuilder().build();

    public static <E> Page<E> create(Function<Page<E>, List<E>> function) {
        return create(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, function);
    }

    public static <E> Page<E> create(int pageNo, Function<Page<E>, List<E>> function) {
        return create(pageNo, DEFAULT_PAGE_SIZE, function);
    }

    public static <E> Page<E> create(int pageNo, int pageSize, Function<Page<E>, List<E>> function) {
        Page<E> result = new Page<>();
        if (pageSize > 0)
            result.setPageSize(pageSize);
        result.setPageNo(pageNo <= 0 ? 1 : pageNo);
        result.setResults(function.apply(result));
        List<E> results = result.results;
        Optional.ofNullable(results).ifPresent(es -> {
            result.setCount(results.size());
            result.setTotalPage(results.size() == 0 ? 0 : (results.size() - 1) / result.pageSize + 1);
        });
        return result;
    }

    public static <E> Page<E> create(Class<E> e, int pageSize, Function<Page<E>, List<E>> function) {
        return create(1, pageSize, function);
    }

    public static <E> Page<E> create(Class<E> e, Function<Page<E>, List<E>> function) {
        return create(1, 60, function);
    }

    public static <E> Page<E> create(List<E> list) {
        return create(1, 60, ePage -> list);
    }

    public static <E> Page<E> create(Integer pageSize, List<E> list) {
        return create(1, pageSize, ePage -> list);
    }

    public static <E> Page<E> createForImpala(int pageNo, int pageSize, int total, Function<Page<E>, List<E>> function) {
        Page<E> result = new Page<>();
        if (pageSize > 0)
            result.setPageSize(pageSize);
        result.setPageNo(pageNo <= 0 ? 1 : pageNo);
        List<E> list = function.apply(result);
        result.setResults(list);
        result.setCount(total);
        result.setTotalPage(result.results.size() == 0 ? 0 : (result.results.size() - 1) / result.pageSize + 1);
        return result;
    }

    public void forEach(Consumer<List<T>> action) {
        if (totalPage == 0) return;
        IntStream.rangeClosed(pageNo, totalPage).forEach(i -> {
            if (i == totalPage)
                action.accept(results.subList((pageSize * i - pageSize), results.size()));
            else
                action.accept(results.subList((pageSize * i - pageSize), i * pageSize));
        });
    }

    public void forEachParallel(Consumer<List<T>> action) {
        if (totalPage == 0) return;
        IntStream.rangeClosed(pageNo, totalPage).parallel().forEach(i -> {
            if (i == totalPage)
                action.accept(results.subList((pageSize * i - pageSize), results.size()));
            else
                action.accept(results.subList((pageSize * i - pageSize), i * pageSize));
        });
    }

    private void getFromPageNum(Integer pageNum) {

    }
}
