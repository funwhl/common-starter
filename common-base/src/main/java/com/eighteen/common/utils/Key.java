package com.eighteen.common.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by wangwei.
 * Date: 2019/10/26
 * Time: 4:06
 */
public class Key {
    public static byte [] getBucketId(byte [] key, Integer bit) {
        MessageDigest mdInst = null;
        try {
            mdInst = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        mdInst.update(key);
        byte [] md = mdInst.digest();
        byte [] r = new byte[(bit-1)/7 + 1];// 因为一个字节中只有7位能够表示成单字符
        int a = (int) Math.pow(2, bit%7)-2;
        md[r.length-1] = (byte) (md[r.length-1] & a);
        System.arraycopy(md, 0, r, 0, r.length);
        for(int i=0;i<r.length;i++) {
            if(r[i]<0) r[i] &= 127;
        }
        return r;
    }

}
