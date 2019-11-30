package com.eighteen.common.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.SocketTimeoutException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Created by jie on 16-8-14.
 */
public class HttpClientUtils {

    public static final int connTimeout = 5000;
    public static final int readTimeout = 5000;
    public static final String charset = "UTF-8";
    private static final int maxTole = 200;
    public static HttpClient client = null;

    static {

        // 需要通过以下代码声明对https连接支持
        SSLContext sslcontext = null;
        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
            HostnameVerifier hostnameVerifier = NoopHostnameVerifier.INSTANCE;
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext, hostnameVerifier);
            Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", PlainConnectionSocketFactory.getSocketFactory())
                    .register("https", sslsf)
                    .build();

            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            cm.setMaxTotal(maxTole);
            cm.setDefaultMaxPerRoute(maxTole);
            client = HttpClients.custom().setConnectionManager(cm).build();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
    }

    public static String postJson(String url, String parameterStr, Map<String, String> headers) throws Exception {
        return post(url, parameterStr, headers, "application/json", charset, connTimeout, readTimeout);
    }

    public static String postJson(String url, String parameterStr) throws Exception {
        return post(url, parameterStr, "application/json", charset, connTimeout, readTimeout);
    }

    public static String postParameters(String url, String parameterStr) throws Exception {
        return post(url, parameterStr, "application/x-www-form-urlencoded", charset, connTimeout, readTimeout);
    }

    public static String postParameters(String url, String parameterStr, String charset, Integer connTimeout,
                                        Integer readTimeout) throws Exception {
        return post(url, parameterStr, "application/x-www-form-urlencoded", charset, connTimeout, readTimeout);
    }

    public static String postParameters(String url, Map<String, String> params) throws Exception {
        return postForm(url, params, null, connTimeout, readTimeout);
    }

    public static String postParameters(String url, Map<String, String> params, Integer connTimeout, Integer readTimeout)
            throws Exception {
        return postForm(url, params, null, connTimeout, readTimeout);
    }

    public static String postParameters(String url, File file, String fileParam) throws Exception {
        return postForm(url, file, fileParam, null, connTimeout, readTimeout);
    }

    public static String get(String url) throws Exception {
        return get(url, charset, null, null);
    }

    public static String get(String url, String charset, int connTimeout) throws Exception {
        return get(url, charset, connTimeout, readTimeout);
    }

    /**
     * 发送一个 Post 请求, 使用指定的字符集编码.
     *
     * @param url
     * @param body        RequestBody
     * @param mimeType    例如 application/xml "application/x-www-form-urlencoded"
     *                    a=1&b=2&c=3
     * @param charset     编码
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return ResponseBody, 使用指定的字符集编码.
     * @throws ConnectTimeoutException 建立链接超时异常
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String post(String url, String body, String mimeType, String charset, Integer connTimeout,
                              Integer readTimeout) throws Exception {

        HttpPost post = createPostByBody(url, body, mimeType, charset, connTimeout, readTimeout);
        String result;
        try {
            HttpResponse res = client.execute(post);
            result = EntityUtils.toString(res.getEntity(), charset);
        } finally {
            post.releaseConnection();
        }
        return result;
    }

    /**
     * 发送一个 Post 请求, 使用指定的字符集编码.
     *
     * @param url
     * @param body        RequestBody
     * @param mimeType    例如 application/xml "application/x-www-form-urlencoded"
     *                    a=1&b=2&c=3
     * @param charset     编码
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return ResponseBody, 使用指定的字符集编码.
     * @throws ConnectTimeoutException 建立链接超时异常
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String post(String url, String body, Map<String, String> headers, String mimeType, String charset, Integer connTimeout,
                              Integer readTimeout) throws Exception {

        HttpPost post = createPostByBody(url, body, mimeType, charset, connTimeout, readTimeout);
        String result;
        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> entry : headers.entrySet()) {
                post.addHeader(entry.getKey(), entry.getValue());
            }
        }
        try {
            HttpResponse res = client.execute(post);
            result = EntityUtils.toString(res.getEntity(), charset);
        } finally {
            post.releaseConnection();
        }
        return result;
    }


    public static HttpPost createPostByBody(String url, String body, String mimeType, String charset, Integer connTimeout,
                                            Integer readTimeout) {
        HttpPost post = new HttpPost(url);
        if (StringUtils.isNotBlank(body)) {
            HttpEntity entity = new StringEntity(body, ContentType.create(mimeType, charset));
            post.setEntity(entity);
        }
        // 设置参数
        RequestConfig.Builder customReqConf = RequestConfig.custom();
        if (connTimeout != null) {
            customReqConf.setConnectTimeout(connTimeout);
        }
        if (readTimeout != null) {
            customReqConf.setSocketTimeout(readTimeout);
        }
        post.setConfig(customReqConf.build());
        return post;
    }

    /**
     * 提交form表单
     *
     * @param url
     * @param params
     * @param connTimeout
     * @param readTimeout
     * @return
     * @throws ConnectTimeoutException
     * @throws SocketTimeoutException
     * @throws Exception
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers,
                                  Integer connTimeout, Integer readTimeout) throws Exception {

        HttpPost post = createPost(url, params, headers, connTimeout, readTimeout);
        try {
            HttpResponse res = client.execute(post);
            // return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
            return EntityUtils.toString(res.getEntity(), charset);
        } finally {
            post.releaseConnection();
        }
    }

    /**
     * 返回文件
     *
     * @param url
     * @param fileType    like png, txt
     * @param readTimeout
     * @return
     * @throws Exception
     */
    public static File getFileByPost(String url, String jsonParams, String fileType, Integer readTimeout) throws Exception {

        File file = File.createTempFile("download_", "." + fileType);
        HttpPost post = createPostByBody(url, jsonParams, "application/x-www-form-urlencoded", charset, connTimeout, readTimeout);
        try {
            HttpResponse res = client.execute(post);
            InputStream input = res.getEntity().getContent();
            OutputStream output = new FileOutputStream(file);
            copyLarge(input, output);
            output.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            post.releaseConnection();
        }
        System.out.println("file:" + file.getPath());
        return file;
    }

    private static HttpPost createPost(String url, Map<String, String> params, Map<String, String> headers,
                                       Integer connTimeout, Integer readTimeout) throws Exception {
        HttpPost post = new HttpPost(url);
        if (params != null && !params.isEmpty()) {
            List<NameValuePair> formParams = new ArrayList<NameValuePair>();
            Set<Entry<String, String>> entrySet = params.entrySet();
            for (Entry<String, String> entry : entrySet) {
                formParams.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formParams, Consts.UTF_8);
            post.setEntity(entity);
        }
        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> entry : headers.entrySet()) {
                post.addHeader(entry.getKey(), entry.getValue());
            }
        }
        // 设置参数
        RequestConfig.Builder customReqConf = RequestConfig.custom();
        if (connTimeout != null) {
            customReqConf.setConnectTimeout(connTimeout);
        }
        if (readTimeout != null) {
            customReqConf.setSocketTimeout(readTimeout);
        }
        post.setConfig(customReqConf.build());
        return post;
    }


    private static HttpGet createGet(String url, Map<String, String> headers,
                                     Integer connTimeout, Integer readTimeout) throws Exception {
        HttpGet get = new HttpGet(url);

        if (headers != null && !headers.isEmpty()) {
            for (Entry<String, String> entry : headers.entrySet()) {
                get.addHeader(entry.getKey(), entry.getValue());
            }
        }
        // 设置参数
        RequestConfig.Builder customReqConf = RequestConfig.custom();
        if (connTimeout != null) {
            customReqConf.setConnectTimeout(connTimeout);
        }
        if (readTimeout != null) {
            customReqConf.setSocketTimeout(readTimeout);
        }
        get.setConfig(customReqConf.build());
        return get;
    }


    /**
     * 提交form表单
     *
     * @param url
     * @param
     * @param connTimeout
     * @param readTimeout
     * @return
     * @throws ConnectTimeoutException
     * @throws SocketTimeoutException
     * @throws Exception
     */
    public static String postForm(String url, File file, String fileParam, Map<String, String> headers,
                                  Integer connTimeout, Integer readTimeout) throws Exception {

        HttpPost post = new HttpPost(url);
        try {
            if (file == null) {
                return null;
            }
            MultipartEntityBuilder reqEntity = MultipartEntityBuilder.create();
            reqEntity.addBinaryBody(fileParam, file); // 设置文件
            post.setEntity(reqEntity.build());

            if (headers != null && !headers.isEmpty()) {
                for (Entry<String, String> entry : headers.entrySet()) {
                    post.addHeader(entry.getKey(), entry.getValue());
                }
            }
            // 设置参数
            RequestConfig.Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            post.setConfig(customReqConf.build());
            HttpResponse res = client.execute(post);
            // return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
            return EntityUtils.toString(res.getEntity(), charset);
        } finally {
            post.releaseConnection();
        }
    }

    public static String get(String url, Map<String, String> headers) throws Exception {
        HttpGet get = createGet(url, headers, 3000, 3000);
        try {
            HttpResponse res = client.execute(get);
            // return IOUtils.toString(res.getEntity().getContent(), "UTF-8");
            return EntityUtils.toString(res.getEntity(), charset);
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * 发送一个 GET 请求
     *
     * @param url
     * @param charset
     * @param connTimeout 建立链接超时时间,毫秒.
     * @param readTimeout 响应超时时间,毫秒.
     * @return
     * @throws ConnectTimeoutException 建立链接超时
     * @throws SocketTimeoutException  响应超时
     * @throws Exception
     */
    public static String get(String url, String charset, Integer connTimeout, Integer readTimeout) throws Exception {
        HttpGet get = new HttpGet(url);
        String result = null;
        try {
            // 设置参数
            RequestConfig.Builder customReqConf = RequestConfig.custom();
            if (connTimeout != null) {
                customReqConf.setConnectTimeout(connTimeout);
            }
            if (readTimeout != null) {
                customReqConf.setSocketTimeout(readTimeout);
            }
            get.setConfig(customReqConf.build());
            HttpResponse res = client.execute(get);
            result = EntityUtils.toString(res.getEntity(), charset);
        } finally {
            get.releaseConnection();
        }
        return result;
    }

    /**
     * 下载文件
     *
     * @param url
     * @param fileType 文件类型， txt jpg 等
     * @return
     * @throws ConnectTimeoutException
     * @throws SocketTimeoutException
     * @throws Exception
     */
    public static File get(String url, String fileType) throws Exception {

        HttpGet get = new HttpGet(url);
        File file = File.createTempFile("download_", "." + fileType);
        try {
            // 设置参数
            RequestConfig.Builder customReqConf = RequestConfig.custom();
            customReqConf.setConnectTimeout(connTimeout);
            get.setConfig(customReqConf.build());
            HttpResponse res = client.execute(get);
            InputStream input = res.getEntity().getContent();
            OutputStream output = new FileOutputStream(file);
            copyLarge(input, output);
            output.flush();
        } finally {
            get.releaseConnection();
        }
        return file;
    }

    /**
     * 拷贝字节流
     * TODO Add comments here.
     *
     * @param input
     * @param output
     * @return
     * @throws IOException
     */
    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024 * 4];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
