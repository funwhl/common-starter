package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import org.jooq.Configuration;
import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.jooq.impl.DAOImpl;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 5:03
 */
public class DAO<R extends UpdatableRecord<R>, P, T> extends DAOImpl<R, P, T> {
    protected DAO(Table table, Class type) {
        super(table, type);
    }

    protected DAO(Table table, Class type, Configuration configuration) {
        super(table, type, configuration);
    }

    @Override
    protected Object getId(Object o) {
        return null;
    }
}
