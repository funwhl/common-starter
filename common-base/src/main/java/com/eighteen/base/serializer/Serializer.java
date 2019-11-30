package com.eighteen.base.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by eighteen.
 * Date: 2019/8/21
 * Time: 22:50
 */

public interface Serializer {

    <T> T read(InputStream stream, Class<T> clazz);

    default <T> T read(byte[] bytes, Class<T> clazz) {
        return read(new ByteArrayInputStream(bytes), clazz);
    }

    void write(OutputStream stream, Object o);

    default byte[] write(Object o) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, o);
        return out.toByteArray();
    }
}
