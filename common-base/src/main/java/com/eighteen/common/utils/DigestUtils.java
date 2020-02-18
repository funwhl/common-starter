package com.eighteen.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by wangwei.
 * Date: 2020/2/18
 * Time: 18:48
 */
public class DigestUtils {
    public static String getMd5Str(String value) {
        if (StringUtils.isNotBlank(value))
            return org.springframework.util.DigestUtils.md5DigestAsHex(value.getBytes());
        else return "";
    }
}
