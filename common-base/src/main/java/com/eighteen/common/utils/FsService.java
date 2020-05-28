package com.eighteen.common.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.eighteen.common.utils.ImageUtils.graphicsGeneration;

/**
 * Created by wangwei.
 * Date: 2020/3/2
 * Time: 21:37
 */
public class FsService {
    private String appId;
    private String appSecret;
    private Cache<String, String> tenantToken = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).concurrencyLevel(1)
            .build();

    public static Cache<Integer, String> duplicateCache = CacheBuilder.newBuilder()
            .expireAfterWrite(2, TimeUnit.MINUTES).concurrencyLevel(1)
            .build();

    public FsService(String appId, String appSecret) {
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String send(String imageId) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + getTenantToken());
        map.put("Content-Type", "application/json");
        String text = "{\"msg_type\":\"post\",\"email\":\"wangwei@angogo.cn\",\"content\":{\"post\":{\"zh_cn\":{\"title\":\"feedback\",\"content\":[[{\"tag\":\"text\",\"text\":\"\"},{\"un_escape\":true,\"tag\":\"text\",\"text\":\"" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss") + "\"}],[{\"un_escape\":true,\"tag\":\"text\",\"text\":\"text&nbsp;:\"},{\"tag\":\"a\",\"text\":\"点击查看   \",\"href\":\"http://owl.18daxue.net//html/stat/DaylyIncomeComparelist.html\"},{\"user_id\":\"all\",\"tag\":\"at\"}],[{\"image_key\":\"" + imageId + "\",\"width\":400,\"tag\":\"img\",\"height\":400}]]}}}}";
        return HttpClientUtils.postJson("https://open.feishu.cn/open-apis/message/v4/send/", text, map);
    }

    public String generaterImage(List<Map<String, Object>> list, Map<String, Object> headMap) throws Exception {
        Map<String, String> map = new HashMap<>(5);
        map.put("Authorization", "Bearer " + getTenantToken());
        List<List<List<String>>> allValue = new ArrayList<>();

        list = list.stream().map(o -> {
            LinkedHashMap<String, Object> sorted = new LinkedHashMap<>();
            headMap.forEach((k, v) -> sorted.put(k, o.get(k)));
            return sorted;
        }).collect(Collectors.toList());

        List<List<String>> contents2 = new ArrayList<>();
        List<String> heads = new ArrayList<>();
        heads.add("产品");
        List<Map<String, Object>> finalList = list;
        list.get(0).forEach((s, o) -> {
            List<String> colum = new ArrayList<>();
            for (int i = 0; i < finalList.size(); i++) {
                String value = String.valueOf(finalList.get(i).get(s));
                if (s.equals("prodname")) {
                    heads.add(value);
                } else {
                    if (i == 0) {
                        colum.add(headMap.get(s) == null ? s : headMap.get(s).toString());
                    }
                    colum.add(value);
                }
            }
            if (colum.size() > 0 && !s.equals("dt")) contents2.add(colum);
        });

        allValue.add(contents2);
        List<String[]> headTitles = new ArrayList<>();
        headTitles.add(heads.toArray(new String[]{}));
        List<String> titles = new ArrayList<>();
        titles.add("今日数据指标");

        File file = graphicsGeneration(allValue, titles, headTitles, "", 4);
        return SendImageByApacheHttpClient(file, "Bearer " + getTenantToken());
    }

    public CloseableHttpClient getHttpClient() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(builder.build(),
                    NoopHostnameVerifier.INSTANCE);
            Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", sslConnectionSocketFactory)
                    .build();
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(registry);
            cm.setMaxTotal(100);
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .setDefaultCookieStore(new BasicCookieStore())
                    .setConnectionManager(cm).build();
            return httpclient;
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return HttpClients.createDefault();
    }

    public String SendImageByApacheHttpClient(File file, String aothorization) throws IOException {
        CloseableHttpClient client = getHttpClient();
        HttpPost post = new HttpPost("https://open.feishu.cn/open-apis/image/v4/put/");
        final MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        FileBody bin = new FileBody(file);
        builder.addPart("image", bin);
        builder.addTextBody("image_type", "message");
        HttpEntity multiPartEntity = builder.build();
        post.setEntity(multiPartEntity);
        post.setHeader("Authorization", aothorization);
        CloseableHttpResponse response = client.execute(post);
        System.out.println("http response code:" + response.getStatusLine().getStatusCode());
        for (Header header : response.getAllHeaders()) {
            System.out.println(header.toString());
        }
        HttpEntity resEntity = response.getEntity();
        if (resEntity == null) {
            System.out.println("never here?");
            return "";
        }
        System.out.println("Response content length: " + resEntity.getContentLength());
        return EntityUtils.toString(resEntity);
    }

    public String getTenantToken() {
       return Optional.ofNullable(this.tenantToken.getIfPresent("tenantToken")).orElseGet(() -> {
            Map<String, String> map = new HashMap<>(2);
            map.put("app_id", appId);
            map.put("app_secret", appSecret);
           String json = null;
           try {
               json = HttpClientUtils.postJson(FsHost.TENANT_ACCESS_TOKEN_URL, JSONObject.toJSONString(map));
           } catch (Exception e) {
               e.printStackTrace();
           }
           String tenant_access_token = JSONObject.parseObject(json).getString("tenant_access_token");
           tenantToken.put("tenantToken",tenant_access_token);
           return tenant_access_token;
        });
    }

    public String getChatList() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + getTenantToken());
        map.put("Content-Type", "application/json");
        String json = HttpClientUtils.postForm(FsHost.CHAT_LIST_URL, null, map, 3000, 3000);
        return json;
    }

    public JSONArray getChatUserList(String chatId) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + getTenantToken());
        map.put("Content-Type", "application/json");
        String json = HttpClientUtils.postForm(FsHost.CHAT_INFO + chatId, null, map, 3000, 3000);
        if (!json.contains("members\":")) {
            throw new Exception("获取群信息失败！" + json);
        }
        JSONArray arr = JSONObject.parseObject(json).getJSONObject("chat").getJSONArray("members");
        return arr;
    }

    public void sendChatUserList(JSONArray openids, String msg) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + getTenantToken());
        map.put("Content-Type", "application/json");
        JSONObject paramMap = new JSONObject();
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("text", "<at user_id=\"all\"> </at> " + msg);
        paramMap.put("open_ids", openids);
        paramMap.put("msg_type", "text");
        paramMap.put("content", jsonObj);
        String json = HttpClientUtils.postJson(FsHost.BATCH_SEND_USER_MESSAGE, JSONObject.toJSONString(paramMap), map);
        return;
    }

    public void sendChatMsg(String chatId, String msg) throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + getTenantToken());
        map.put("Content-Type", "application/json");

        JSONObject paramMap = new JSONObject();
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("text", "<at user_id=\"all\"> </at> " + msg);
        paramMap.put("open_chat_id", chatId);
        paramMap.put("msg_type", "text");
        paramMap.put("content", jsonObj);

        String json = HttpClientUtils.postJson(FsHost.SEND_CHAT_MESSAGE, JSONObject.toJSONString(paramMap), map);
        if (!json.contains("open_message_id")) {
            throw new Exception("发送群消息失败！" + json);
        }

    }
    public void sendMsg(String msg) {
        sendMsg("wangnwei@angogo.cn",msg);
    }

    public void sendMsgNoDuplicate(String msg) {
        String presentMsg = duplicateCache.getIfPresent(msg.hashCode());
        if (StringUtils.isBlank(presentMsg)) {
            sendMsg("wangnwei@angogo.cn",msg);
            duplicateCache.put(msg.hashCode(),"1");
        }
    }

    public void sendMsg(String email, String msg){
        try {
            Map<String, String> map = new HashMap<>();
            map.put("Authorization", "Bearer " + getTenantToken());
            map.put("Content-Type", "application/json");

            JSONObject paramMap = new JSONObject();
            JSONObject jsonObj = new JSONObject();
            jsonObj.put("text", msg);
            paramMap.put("email", email);
            paramMap.put("msg_type", "text");
            paramMap.put("content", jsonObj);

            String json = HttpClientUtils.postJson(FsHost.SEND_CHAT_MESSAGE, JSONObject.toJSONString(paramMap), map);
            if (!json.contains("open_message_id")) {
                throw new Exception("发送用户消息失败！json:" + json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getAppToken() throws Exception {
        Map<String, String> map = new HashMap<>();
        map.put("app_id", appId);
        map.put("app_secret", appSecret);
        String json = HttpClientUtils.postParameters(FsHost.APP_ACCESS_TOKEN_URL, map);
        JSONObject jsonObject = JSONObject.parseObject(json);
        return jsonObject.getString("app_access_token");
    }

    interface FsHost {
        public final String CHAT_INFO = "https://open.feishu.cn/open-apis/chat/v3/info?open_chat_id=";

        public final String CHAT_LIST_URL = "https://open.feishu.cn/open-apis/chat/v3/list?page=1&page_size=50";

        public final String SEND_CHAT_MESSAGE = "https://open.feishu.cn/open-apis/message/v3/send/";

        public final String BATCH_SEND_USER_MESSAGE = "https://open.feishu.cn/open-apis/message/v3/batch_send/";
        public final String APP_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal/";

        public final String TENANT_ACCESS_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal/";

    }

}
