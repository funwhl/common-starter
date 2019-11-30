package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import org.jooq.*;
import org.jooq.exception.NoDataFoundException;
import org.jooq.impl.DSL;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.jooq.impl.DSL.using;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:10
 */
public class JOOQGenericDao<T, ID extends Serializable> implements GenericDao<T, ID> {

    private Table<? extends Record> table = null;
    private Class<T> entityClass = null;
    private Configuration configuration = null;
    private Field<ID> primaryKey = null;

    public JOOQGenericDao(Class<T> entityClass, Schema schema, Configuration configuration) {
        this.entityClass = entityClass;
        this.configuration = configuration;
        initTable(schema);
        primaryKey = pk();
    }

    @Override
    public T insert(T entity) {
        record(entity, false, using(configuration)).store();
        return entity;
    }

    @Override
    public void insert(Collection<T> entities) {
        if (entities.isEmpty()) {
            return;
        }

        List<UpdatableRecord<?>> rs = records(entities, false);

        if (rs.size() > 1) {
            using(configuration).batchInsert(rs).execute();
            return;
        }

        rs.get(0).insert();
    }

    @Override
    public T update(T entity) {
        record(entity, true, using(configuration)).update();
        return entity;
    }

    @Override
    public void update(Collection<T> entities) {
        if (entities.isEmpty()) {
            return;
        }

        List<UpdatableRecord<?>> rs = records(entities, false);

        if (rs.size() > 1) {
            using(configuration).batchUpdate(rs).execute();
            return;
        }

        rs.get(0).update();
    }

    @Override
    public int delete(ID id) {
        return using(configuration).delete(table).where(primaryKey.equal(id)).execute();
    }

    @Override
    public void delete(Collection<ID> ids) {
        using(configuration).delete(table).where(primaryKey.in(ids)).execute();
    }

    @Override
    public void delete(Condition... conditions) {
        delete(Stream.of(conditions));
    }

    @Override
    public void deleteWithOptional(Stream<Optional<Condition>> conditions) {
        delete(conditions.filter(Optional::isPresent).map(Optional::get));
    }

    @Override
    public void delete(Stream<Condition> conditions) {
        Optional<Condition> o = conditions.reduce((acc, item) -> acc.and(item));
        Condition c = o.orElseThrow(() -> new IllegalArgumentException("At least one condition is needed to perform deletion"));
        using(configuration).delete(table).where(c).execute();
    }

    @Override
    public T get(ID id) {
        return getOptional(id).orElseThrow(() -> new NoDataFoundException("No entity found by id:" + id));
    }

    @Override
    public Optional<T> getOptional(ID id) {
        Record record = using(configuration).select().from(table).where(primaryKey.eq(id)).fetchOne();
        return Optional.ofNullable(record).map(r -> r.into(entityClass));
    }

    @Override
    public List<T> get(Collection<ID> ids) {
        return using(configuration).select().from(table).where(primaryKey.in(ids)).fetch().into(entityClass);
    }

    @Override
    public int count(Condition... conditions) {
        return count(Stream.of(conditions));
    }

    @Override
    public int countWithOptional(Stream<Optional<Condition>> conditions) {
        return count(conditions.filter(Optional::isPresent).map(Optional::get));
    }

    @Override
    public int count(Stream<Condition> conditions) {
        Condition c = conditions.reduce((acc, item) -> acc.and(item)).orElse(DSL.trueCondition());
        return using(configuration).fetchCount(table, c);
    }

    @Override
    public List<T> fetch(Condition... conditions) {
        return fetch(-1, 0, conditions).getItems();
    }

    @Override
    public List<T> fetchWithOptional(Stream<Optional<Condition>> conditions, SortField<?>...sorts) {
        return fetchWithOptional(-1, 0, conditions, sorts).getItems();
    }

    @Override
    public List<T> fetch(Stream<Condition> conditions, SortField<?>...sorts) {
        return fetch(-1, 0, conditions, sorts).getItems();
    }

    @Override
    public PagedResult<T> fetch(int page, int pageSize, Condition... conditions) {
        return fetch(page, pageSize, Stream.of(conditions));
    }

    @Override
    public PagedResult<T> fetchWithOptional(int page, int pageSize, Stream<Optional<Condition>> conditions, SortField<?>...sorts) {
        return fetch(page, pageSize, conditions.filter(Optional::isPresent).map(Optional::get), sorts);
    }

    @Override
    public PagedResult<T> fetch(int page, int pageSize, Stream<Condition> conditions, SortField<?>...sorts) {
        Condition c = conditions.reduce((acc, item) -> acc.and(item)).orElse(DSL.trueCondition());
        SelectSeekStepN<Record> step = using(configuration).select().from(table).where(c).orderBy(sorts);
        if (page > 0) {
            int firstResult = (page - 1) * pageSize;
            List<T> items = step.limit(firstResult, pageSize).fetch().into(entityClass);
            return PagedResult.create(count(c), items);
        }

        List<T> items = step.fetch().into(entityClass);
        return PagedResult.create(-1, items);
    }

    @Override
    public Optional<T> fetchOne(Condition... conditions) {
        return fetchOne(Stream.of(conditions));
    }

    @Override
    public Optional<T> fetchOneWithOptional(Stream<Optional<Condition>> conditions) {
        return fetchOne(conditions.filter(Optional::isPresent).map(Optional::get));
    }

    @Override
    public Optional<T> fetchOne(Stream<Condition> conditions) {
        List<T> list = fetch(0, 1, conditions).getItems();
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    @Override
    public <O> O execute(Executor<O> cb) {
        return cb.execute(using(configuration));
    }

    @Override
    public PagedResult<T> fetchPage(int page, int pageSize, Executor<SelectLimitStep<?>> e, RecordMapper<Record, T> mapper) {
        DSLContext context = using(configuration);
        SelectLimitStep<?> r = e.execute(context);
        int count = context.fetchCount(r);
        List<T> list = r.limit((page - 1) * pageSize, pageSize).fetch(mapper);
        return PagedResult.create(count, list);
    }

    @Override
    public PagedResult<T> fetchPage(int page, int pageSize, Collection<Field<?>> fields, Function<SelectSelectStep<?>, SelectWhereStep<?>> ss, Optional<Condition> condition, Collection<SortField<?>> sorts, RecordMapper<Record, T> mapper) {
        DSLContext context = using(configuration);
        SelectSeekStepN<?> r = ss.apply(context.selectDistinct(fields)).where(condition.orElse(DSL.trueCondition())).orderBy(sorts);
        int count = ss.apply(context.select(DSL.count())).where(condition.orElse(DSL.trueCondition())).fetchOne(DSL.count());
        List<T> list = r.limit((page - 1) * pageSize, pageSize).fetch(mapper);
        return PagedResult.create(count, list);
    }

    private void initTable(Schema schema) {
        Class<?> is = findInterface(entityClass).orElseThrow(() -> new RuntimeException("Entity class must implements one interface at least."));
        table = schema.getTables().stream()
                .filter(t -> is.isAssignableFrom(t.getRecordType())).findFirst()
                .orElseThrow(() -> new RuntimeException("Can't find a table for the entity."));
    }

    @SuppressWarnings("unchecked")
    private Field<ID> pk() {
        UniqueKey<?> uk = table.getPrimaryKey();
        Field<?>[] fs = uk.getFieldsArray();
        return (Field<ID>)fs[0];
    }

    private List<UpdatableRecord<?>> records(Collection<T> objects, boolean forUpdate) {
        DSLContext context = using(configuration);
        return objects.stream().map(obj -> record(obj, forUpdate, context)).collect(Collectors.toList());
    }

    private UpdatableRecord<?> record(T object, boolean forUpdate, DSLContext context) {
        UpdatableRecord<?> r = (UpdatableRecord<?>)context.newRecord(table, object);
        if (forUpdate) {
            r.changed(primaryKey, false);
        }

        int size = r.size();

        for (int i = 0; i < size; i++) {
            if (r.getValue(i) == null && !r.field(i).getDataType().nullable()) {
                r.changed(i, false);
            }
        }
        return r;
    }

    private Optional<Class<?>> findInterface(Class<?> clazz){
        if(Object.class == clazz){
            return Optional.empty();
        }
        Class<?>[] is = clazz.getInterfaces();
        for(Class<?> c : is){
            if(c.getSimpleName().startsWith("I")){
                return Optional.of(c);
            }
        }
        return findInterface(clazz.getSuperclass());
    }
}
