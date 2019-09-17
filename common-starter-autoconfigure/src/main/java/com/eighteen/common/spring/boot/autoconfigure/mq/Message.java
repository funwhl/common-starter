package com.eighteen.common.spring.boot.autoconfigure.mq;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by wangwei.
 * Date: 2019/8/21
 * Time: 23:29
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private int type = -1;

    private Object payload = null;
    private Map<String, String> headers;
    private long createTime = System.currentTimeMillis();

    public Message() {
        headers = new HashMap<>();
    }

    public int getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
    public <T> T getPayload(Class<T> aClass) {
        return (T) payload;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public boolean match(int type) {
        return this.type == type;
    }

    public Message addHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public long getCreateTime() {
        return createTime;
    }

    public static Message create(int type, Object payload, String... headers) {
        Message m = new Message();
        m.type = type;
        m.payload = payload;

        Arrays.stream(headers).peek(Objects::requireNonNull).reduce(null, (acc, item) -> {
            if (acc == null) {
                return item;
            }
            m.addHeader(acc, item);
            return null;
        });

        return m;
    }

    @Override
    public String toString() {
        return String.format("Message#%s Headers: %s Payload: %s CreateTime: %s", getType(), getHeaders(), getPayload(), getCreateTime());
    }
}
