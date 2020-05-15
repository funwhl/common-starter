package com.eighteen.common.distribution;

import com.eighteen.common.distribution.exception.SerialNumberException;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicInteger;
import org.apache.curator.retry.RetryNTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * Created by wangwei.
 * Date: 2020/3/6
 * Time: 22:52
 */
public class DistributedSerialNumber {
    public static final String PATH = "/numbers";

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedSerialNumber.class);

    private String projectPrefix = null;
    private ZooKeeperConnector zooKeeperConnector = null;
    private LoadingCache<String, DistributedAtomicInteger> cache = null;
    private DateTimeFormatter formatter = null;

    public DistributedSerialNumber() {
        formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        final RetryPolicy policy = new RetryNTimes(5, 1000);
        cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
                .build(new CacheLoader<String, DistributedAtomicInteger>() {
                    @Override
                    public DistributedAtomicInteger load(String key) throws Exception {
                        return new DistributedAtomicInteger(zooKeeperConnector.connect(), key, policy);
                    }
                });
    }

    public void setZooKeeperConnector(ZooKeeperConnector zooKeeperConnector) {
        this.zooKeeperConnector = zooKeeperConnector;
    }

    public void setProjectPrefix(String projectPrefix) {
        this.projectPrefix = projectPrefix;
    }

    public String next(String prefix) {
        return getNextId(prefix, true);
    }

    public String tryNext(String prefix) {
        return getNextId(prefix, false);
    }

    private String getNextId(String prefix, boolean required) {
        String date = LocalDate.now().format(formatter);
        try {
            AtomicValue<Integer> value = cache.get(getPath(prefix, date)).increment();
            if (value.succeeded()) {
                return composeResult(prefix, value.postValue(), date);
            }
        } catch (Exception e) {
            if (required) {
                throw new SerialNumberException(e);
            } else {
                LOGGER.error("Can't generate an serial-number", e);
            }
        }
        return null;
    }

    private String getPath(String prefix, String date) {
        return String.join("/", PATH, prefix, date);
    }

    private String composeResult(String prefix, Integer no, String date) {
        return String.join("", projectPrefix, prefix, date, Strings.padStart(no + "", 5, '0'));
    }
}
