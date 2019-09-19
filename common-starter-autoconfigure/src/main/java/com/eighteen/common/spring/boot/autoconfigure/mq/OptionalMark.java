package com.eighteen.common.spring.boot.autoconfigure.mq;

import java.io.Serializable;

/**
 * Created by wangwei.
 * Date: 2019/8/21
 * Time: 22:52
 */
public class OptionalMark implements Serializable {

    private static final long serialVersionUID = 1L;

    private OptionalMark() {
    }

    public static final OptionalMark MARK = new OptionalMark();
}
