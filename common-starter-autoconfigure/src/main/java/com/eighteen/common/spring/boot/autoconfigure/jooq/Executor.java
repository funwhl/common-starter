package com.eighteen.common.spring.boot.autoconfigure.jooq;

import org.jooq.DSLContext;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:11
 */
@FunctionalInterface
public interface Executor<E> {

    E execute(DSLContext context);

}
