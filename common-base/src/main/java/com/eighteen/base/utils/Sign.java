package com.eighteen.base.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Map;
import java.util.TreeMap;

/**
 * Created by wangwei.
 * Date: 2019/10/19
 * Time: 19:34
 */
public class Sign {
    public static String md5Sign(Map target, String charset) {
        Map treeMap = new TreeMap(target);
        StringBuilder sb = new StringBuilder();
        treeMap.keySet().stream().filter(o -> treeMap.get(o) != null && !treeMap.get(o).equals(""))
                .forEach(o -> sb.append(o).append("=").append(treeMap.get(o)).append("&"));
        sb.substring(0, sb.length() - 1);
        sb.append(charset);
        byte[] bytes = DigestUtils.md5(sb.toString());
        StringBuffer stringBuffer = new StringBuffer();
        for (byte b : bytes) {
            int bt = b & 0xff;
            if (bt < 16) {
                stringBuffer.append(0);
            }
            stringBuffer.append(Integer.toHexString(bt));
        }
        return stringBuffer.toString();
    }

    public static String rsa256Sign(Map target, String privateKey, String charset) {
        try {
            Map treeMap = new TreeMap(target);
            StringBuilder sb = new StringBuilder();
            treeMap.keySet().stream().filter(o -> treeMap.get(o) != null && !treeMap.get(o).equals(""))
                    .forEach(o -> sb.append(o).append("=").append(treeMap.get(o)).append("&"));
            sb.substring(0, sb.length() - 1);
            sb.append(charset);
            String content = sb.toString();
            PrivateKey e = getPrivateKeyFromPKCS8("RSA", privateKey,charset);
            Signature signature = Signature.getInstance("SHA256WithRSA");
            signature.initSign(e);
            if (StringUtils.isEmpty(charset)) {
                signature.update(content.getBytes());
            } else {
                signature.update(content.getBytes(charset));
            }

            byte[] signed = signature.sign();
            return new String(Base64.encodeBase64(signed));
        } catch (Exception var6) {
            return null;
        }
    }

    private static PrivateKey getPrivateKeyFromPKCS8(String algorithm, String key,String charset) throws Exception {
        if (key != null && !StringUtils.isEmpty(algorithm)) {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

            byte[] encodedKey = key.getBytes(charset);
            encodedKey = Base64.decodeBase64(encodedKey);
            return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(encodedKey));
        } else {
            return null;
        }
    }
}
