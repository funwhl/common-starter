package com.eighteen.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * Created by wangwei.
 * Date: 2019/9/22
 * Time: 10:38
 */
@Data
public class Page<T> {
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int DEFAULT_PAGE_NO = 1;

    private int pageNo = DEFAULT_PAGE_NO;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int count;
    private int totalPage;
    private List<T> results;
    private static Cache<String,Page> cache=CacheBuilder.newBuilder().expireAfterWrite(5,TimeUnit.MINUTES).build();
    private Map<String, Object> params = new HashMap<>();

    public static <E> Page<E> create(Function<Page<E>, List<E>> function) {
        return create(DEFAULT_PAGE_NO, DEFAULT_PAGE_SIZE, function);
    }

    public static <E> Page<E> create(int pageNo, Function<Page<E>, List<E>> function) {
        return create(pageNo, DEFAULT_PAGE_SIZE, function);
    }

    public static <E> Page<E> create(int pageNo, int pageSize, Function<Page<E>, List<E>> function) {
        Page<E> result = new Page<>();
        if (pageSize > 0) result.setPageSize(pageSize);
        result.setPageNo(pageNo <= 0 ? 1 : pageNo);
        result.setResults(function.apply(result));
        result.setCount(result.results.size());
        result.setTotalPage((result.results.size() - 1) / result.pageSize + 1);
        return result;
    }

    // 2 10 1-6
    public static <E> Page<E> create(int no, int size,int count, Function<Page<E>, List<E>> function,String key) {
        Page<E> result = new Page<>();
        if (size > 0) result.setPageSize(size);
        int start = (size - 1) * no + 1;
        Page<E> page = cache.getIfPresent(key);
        int pageNo = page.pageNo;
        int pageSize = page.pageSize;

        int start2 = ((pageNo - 1) * pageSize) + 1;

        int end = start + size;
        int end2 = start2 + pageSize;

        result.setPageNo(pageNo <= 0 ? 1 : pageNo);
        result.setResults(function.apply(result));
        result.setCount(count);
        result.setTotalPage((count - 1) / result.pageSize + 1);
        cache.put(key,result);
        return result;
    }

//    public  Page<T> createFromCache(int pageNo, int pageSize) {
//        Page<T> result = new Page<>();
//        cache = CacheBuilder.newBuilder().build();
//        if (pageSize > 0) result.setPageSize(pageSize);
//        result.setPageNo(pageNo <= 0 ? 1 : pageNo);
//        List<T> data = cache.getIfPresent("result");
//        result.setResults(getPage(pageNo,pageSize));
//        cache.put("result",data);
//        result.setCount(count);
//        result.setTotalPage((count - 1) / result.pageSize + 1);
//        return result;
//    }

    public static <E> Page<E> getPage(int no, int size,String key) {
        int start = (size - 1) * no + 1;
        Page<E> page = cache.getIfPresent(key);
        int pageNo = page.pageNo;
        int pageSize = page.pageSize;
        int start2 = ((pageNo - 1) * pageSize) + 1;

        int end = start + size;
        int end2 = start2 + pageSize;

        if (start >= start2 && end <= end2) {
            page.setResults(page.getResults().subList(start,end));
            page.setPageNo(no);
            page.setPageSize(size);
            page.setTotalPage((page.count - 1) / size + 1);
            return page;
        }
        return null;
    }

    public void forEach(Consumer<List<T>> action) {
        IntStream.rangeClosed(pageNo, totalPage).forEach(i -> {
            if (i == totalPage)
                action.accept(results.subList((pageSize * i - pageSize), results.size()));
            else action.accept(results.subList((pageSize * i - pageSize), i * pageSize));
        });
    }

    public void forEachParallel(Consumer<List<T>> action) {
        IntStream.rangeClosed(pageNo, totalPage).parallel().forEach(i -> {
            if (i == totalPage)
                action.accept(results.subList((pageSize * i - pageSize), results.size()));
            else action.accept(results.subList((pageSize * i - pageSize), i * pageSize));
        });
    }


    public static void forEach(Integer count,Integer pageSize,Consumer<Integer> action) {
        IntStream.rangeClosed(1, count / pageSize).parallel().forEachOrdered(action::accept);
    }
}