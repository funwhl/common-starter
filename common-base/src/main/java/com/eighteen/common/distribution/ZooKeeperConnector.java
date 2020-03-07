package com.eighteen.common.distribution;

import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;

import java.util.Optional;

/**
 * Created by wangwei.
 * Date: 2020/3/6
 * Time: 22:53
 */
public class ZooKeeperConnector {
    private String connectString = null;
    private String namespace = null;
    private RetryPolicy retryPolicy = null;
    private int sessionTimeout = 1000;

    private CuratorFramework client = null;

    public ZooKeeperConnector(String connectString, String namespace, RetryPolicy retryPolicy) {
        this.connectString = connectString;
        this.namespace = namespace;
        this.retryPolicy = retryPolicy;
    }

    public void setConnectString(String connectString) {
        this.connectString = connectString;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public void setSessionTimeout(int sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public synchronized CuratorFramework connect() {
        client = Optional.ofNullable(client).orElseGet(() -> CuratorFrameworkFactory.builder()
                .namespace(namespace).connectString(connectString).sessionTimeoutMs(sessionTimeout)
                .retryPolicy(retryPolicy).build());

        CuratorFrameworkState state = client.getState();
        if (state == CuratorFrameworkState.LATENT || state == CuratorFrameworkState.STOPPED) {
            client.start();
        }

        return client;
    }

    public void disconnect() {
        Optional.ofNullable(client).ifPresent(c -> {
            CuratorFrameworkState state = c.getState();
            if (state == CuratorFrameworkState.STARTED) {
                c.close();
            }
        });
    }
}
