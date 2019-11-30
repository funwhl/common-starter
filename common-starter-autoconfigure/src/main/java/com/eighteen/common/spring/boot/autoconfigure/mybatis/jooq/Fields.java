package com.eighteen.common.spring.boot.autoconfigure.mybatis.jooq;

import org.jooq.Field;
import org.jooq.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by eighteen.
 * Date: 2019/9/1
 * Time: 12:11
 */
public class Fields {

    private List<Field<?>> fields = new ArrayList<>();

    public static Fields start() {
        return new Fields();
    }

    public Collection<Field<?>> end() {
        return fields;
    }

    public Fields add(Table<?> table) {
        add(table.fields());
        return this;
    }

    public Fields add(Field<?>...fs) {
        fields.addAll(Arrays.asList(fs));
        return this;
    }

    public Fields remove(Field<?>...fs) {
        Stream.of(fs).map(f -> (Predicate<Field<?>>)f::equals).reduce((s, i) -> s.or(i)).map(s -> s.negate()).ifPresent(s -> {
            fields = fields.stream().filter(s).collect(Collectors.toList());
        });

        return this;
    }
}
