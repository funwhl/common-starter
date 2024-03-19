package com.eighteen.common.utils;

import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SecurityUtil {
    private final static Pattern ENC_PATTERN = Pattern.compile("ENC\\((.*?)\\)");

    public static String encrypt(String password, String value) {
        PooledPBEStringEncryptor encryptOr = new PooledPBEStringEncryptor();
        encryptOr.setConfig(pbeConfig(password));
        return encryptOr.encrypt(value);
    }

    public static String decrypt(String password, String value) {
        PooledPBEStringEncryptor encryptOr = new PooledPBEStringEncryptor();
        encryptOr.setConfig(pbeConfig(password));
        return encryptOr.decrypt(value);
    }

    public static String decryptPattern(String password, String value) {
        try {
            Matcher matcher = ENC_PATTERN.matcher(value);
            StringBuffer result = new StringBuffer();
            while (matcher.find()) {
                String encryptedText = matcher.group(1);
                String decryptedText = decrypt(password, encryptedText);
                matcher.appendReplacement(result, decryptedText);
            }
            matcher.appendTail(result);
            return result.toString();
        } catch (Exception e) {
            return value;
        }
    }

    public static SimpleStringPBEConfig pbeConfig(String password) {
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        config.setPassword(password);
        config.setAlgorithm(StandardPBEByteEncryptor.DEFAULT_ALGORITHM);
        config.setKeyObtentionIterations("1001");
        config.setPoolSize("1");
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        config.setStringOutputType("base64");
        return config;
    }

}
