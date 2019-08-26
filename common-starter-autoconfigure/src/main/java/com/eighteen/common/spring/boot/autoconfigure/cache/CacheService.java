package com.eighteen.common.spring.boot.autoconfigure.cache;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 1:02
 */
public interface CacheService {
    Cache create(String var1);

    Cache create(String var1, int var2);

    Cache create(String var1, int var2, int var3);

    Cache create(String var1, String var2);
}
