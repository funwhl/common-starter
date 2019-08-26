package com.eighteen.common.spring.boot.autoconfigure.cache;

/**
 * Created by eighteen.
 * Date: 2019/8/22
 * Time: 1:01
 */
public abstract class AbstractCacheService implements CacheService {
    private String applicationName = null;

    public AbstractCacheService(String applicationName) {
        this.applicationName = applicationName;
    }

    public Cache create(String moduleName) {
        return this.create(moduleName, -1, -1);
    }

    public Cache create(String moduleName, int expire) {
        return this.create(moduleName, expire, -1);
    }

    public Cache create(String moduleName, int expire, int timeout) {
        return this.doCreate(join(this.applicationName, moduleName), expire, timeout);
    }

    public Cache create(String applicationName, String moduleName) {
        return this.doCreate(join(applicationName, moduleName), -1, -1);
    }

    protected abstract Cache doCreate(String var1, int var2, int var3);

    protected static String join(String f, String s) {
        return f + "#" + s;
    }
}

