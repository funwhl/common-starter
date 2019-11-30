package com.eighteen.common.utils;

import java.io.IOException;
import java.util.Objects;

/**
 * Created by wangwei.
 * Date: 2019/10/4
 * Time: 15:55
 */
@FunctionalInterface
public interface Consumer<T,R> {

    void accept(T t, R r) throws IOException;

    default Consumer<T,R> andThen(Consumer<? super T, ? super R> after) {
        Objects.requireNonNull(after);
        return (T t,R r) -> { accept(t,r); after.accept(t,r); };
    }
}

