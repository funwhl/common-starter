package com.eighteen.common.spring.boot.autoconfigure.jooq;

import org.jooq.*;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:12
 */
public interface GenericDao<T, ID extends Serializable> {

    T insert(T entity);

    void insert(Collection<T> entities);

    T update(T entity);

    void update(Collection<T> entities);

    int delete(ID id);

    void delete(Collection<ID> ids);

    void delete(Condition... conditions);

    void deleteWithOptional(Stream<Optional<Condition>> conditions);

    void delete(Stream<Condition> conditions);

    T get(ID id);

    Optional<T> getOptional(ID id);

    List<T> get(Collection<ID> ids);

    List<T> fetch(Condition... conditions);

    int count(Condition... conditions);

    int countWithOptional(Stream<Optional<Condition>> conditions);

    int count(Stream<Condition> conditions);

    List<T> fetchWithOptional(Stream<Optional<Condition>> conditions, SortField<?>... sorts);

    List<T> fetch(Stream<Condition> conditions, SortField<?>... sorts);

    PagedResult<T> fetch(int page, int pageSize, Condition... conditions);

    PagedResult<T> fetch(int page, int pageSize, Stream<Condition> conditions, SortField<?>... sorts);

    PagedResult<T> fetchWithOptional(int page, int pageSize, Stream<Optional<Condition>> conditions, SortField<?>... sorts);

    Optional<T> fetchOne(Condition... conditions);

    Optional<T> fetchOne(Stream<Condition> conditions);

    Optional<T> fetchOneWithOptional(Stream<Optional<Condition>> conditions);

    <O> O execute(Executor<O> cb);

    PagedResult<T> fetchPage(int page, int pageSize, Executor<SelectLimitStep<?>> e, RecordMapper<Record, T> mapper);

    PagedResult<T> fetchPage(int page, int pageSize, Collection<Field<?>> fields, Function<SelectSelectStep<?>, SelectWhereStep<?>> ss, Optional<Condition> conditions, Collection<SortField<?>> sorts, RecordMapper<Record, T> mapper);

}

