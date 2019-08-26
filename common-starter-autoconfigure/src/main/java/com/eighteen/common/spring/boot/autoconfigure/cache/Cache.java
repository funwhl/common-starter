package com.eighteen.common.spring.boot.autoconfigure.cache;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 1:02
 */

import java.util.function.Supplier;

public interface Cache {
    <T> T get(String var1, Class<T> var2);

    <T> T get(String var1, Supplier<T> var2);

    <T> Object get(String var1, Supplier<T> var2, int var3);

    <T> void set(String var1, T var2);

    <T> void set(String var1, T var2, int var3);

    void clear(String var1);

    void clearByPattern(String var1);

    Long increment(String var1);

    Long increment(String var1, Long var2);

    Long expire(String var1, int var2);
}

