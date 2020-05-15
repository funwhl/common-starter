package com.eighteen.common.distribution.exception;

/**
 * Created by wangwei.
 * Date: 2020/3/6
 * Time: 22:50
 */
public class DistributedLockException extends RuntimeException {

    private static final long serialVersionUID = 1310354032900157002L;

    public DistributedLockException(String message, Throwable e) {
        super(message, e);
    }

    public DistributedLockException(String message) {
        super(message);
    }
}