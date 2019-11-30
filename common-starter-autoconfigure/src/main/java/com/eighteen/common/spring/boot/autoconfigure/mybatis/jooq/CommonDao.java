package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import org.jooq.Configuration;
import org.jooq.Schema;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:09
 */
public class CommonDao<E> extends JOOQGenericDao<E, String> {

    public CommonDao(Class<E> entityClass, Schema schema, Configuration configuration) {
        super(entityClass, schema, configuration);
    }

}
