package com.eighteen.common.distribution;

import com.eighteen.common.distribution.exception.DistributedLockException;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by wangwei.
 * Date: 2020/3/6
 * Time: 22:52
 */
public class DistributedLock {
    public static final String PATH = "/locks";

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLock.class);

    private ZooKeeperConnector zooKeeperConnector = null;
    private long timeout = 1000l;

    public void setZooKeeperConnector(ZooKeeperConnector zooKeeperConnector) {
        this.zooKeeperConnector = zooKeeperConnector;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void lock(String group, String id, Runnable task) {
        lock(group, id, true, task);
    }

    public void tryLock(String group, String id, Runnable task) {
        lock(group, id, false, task);
    }

    private void lock(String catalog, String id, boolean required, Runnable task) {
        String path = getPath(catalog, id);
        InterProcessMutex mutex = new InterProcessMutex(zooKeeperConnector.connect(), path);
        try {
            if (mutex.acquire(timeout, TimeUnit.MILLISECONDS)) {
                task.run();
            }
        } catch (Exception e) {
            if (required) {
                throw new DistributedLockException("Can't obtain the lock", e);
            } else {
                LOGGER.error("Can't obtain the lock", e);
            }
        } finally {
            if (mutex.isAcquiredInThisProcess()) {
                try {
                    mutex.release();
                } catch (Exception e) {
                    if (required) {
                        throw new DistributedLockException("Can't release the lock", e);
                    } else {
                        LOGGER.error("Can't release the lock", e);
                    }
                }
            }
        }

    }

    private String getPath(String group, String id) {
        return String.join("/", PATH, group, id);
    }

}
