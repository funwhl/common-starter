package com.eighteen.common.spring.boot.autoconfigure.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.spring.boot.autoconfigure.feedback.dao.FeedBackMapper;
import com.eighteen.common.spring.boot.autoconfigure.feedback.service.FeedbackService;
import com.eighteen.common.spring.boot.autoconfigure.web.HttpClientUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.eighteen.common.spring.boot.autoconfigure.feedback.service.impl.FeedbackServiceImpl.JobType.*;


/**
 * @author : wangwei
 * @date : 2019/8/22 17:43
 */
@Service
public class FeedbackServiceImpl implements FeedbackService {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackServiceImpl.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    @Autowired(required = false)
    private Redis redis;
    @Value("${18.feedback.channel}")
    private String channel;
    @Value("${18.feedback.mode}")
    private int mode;
    @Value("${spring.application.name}")
    private String appName;
    private final static SimpleDateFormat format = new SimpleDateFormat("YYYY-MM-dd");
    private ExecutorService executor = new ThreadPoolExecutor(8, 8,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    @Override
    public void feedback() {
        tryWork(r -> {
            List<String> success = new ArrayList<>();
            List<String> history = new ArrayList<>();
            List<Map<String, Object>> results = feedBackMapper.getPreFetchData(1000);

            Set<String> imeis = feedBackMapper.getDayImeis(new Date(System.currentTimeMillis() -TimeUnit.DAYS.toMillis(2)));

            results = results.stream().sorted((o1, o2) -> ((Date) o2.get("activetime")).compareTo((Date) o1.get("activetime")))
                    .collect(
                            Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> String.valueOf(o2.get("imei"))))), ArrayList::new)
                    );
            if (results != null) results.forEach(o -> {
                Future future = executor.submit(() -> {
                    try {
                        String imei = o.get("imei") == null ? null : String.valueOf(o.get("imei"));
                        if (imeis.contains(imei)) return null;
                        Integer coid = o.get("coid") == null ? null : Integer.valueOf(String.valueOf(o.get("coid")));
                        Integer ncoid = o.get("ncoid") == null ? null : Integer.valueOf(String.valueOf(o.get("ncoid")));
                        if (feedBackMapper.countFromStatistics(imei, coid, ncoid) > 0) {
                            return new String[]{imei, "2"};
                        } else {
                            String url = o.get("call_back") + "&event_type=1&event_time=" + System.currentTimeMillis();
                            String ret = HttpClientUtils.get(url);
                            JSONObject jsonObject = (JSONObject) JSONObject.parse(ret);
                            logger.info("callback->url:{}, return:{}", url, ret);
                            if (jsonObject.get("result").equals(1)) {
                                Map<String, Object> map = new HashMap<>();
                                map.put("CreateTime", new Date());
                                map.put("imei", o.get("imei"));
                                map.put("aid", o.get("aid"));
                                map.put("cid", o.get("cid"));
                                map.put("mac", o.get("mac"));
                                map.put("ip", o.get("ip"));
                                map.put("ts", o.get("ts"));
                                map.put("channel", o.get("channel"));
                                map.put("EventType", 1);
                                map.put("ANDROIDID", o.get("androidId"));
                                map.put("callback_url", url);
//                        try {
//                            jedis.zadd(IMEIS_KEY, System.currentTimeMillis(), imei);
//                        } catch (Exception e) {
//                            logger.error(e.getMessage());
//                        }
                                feedBackMapper.insertFeedback(map);
                            }
                        }
                        return new String[]{imei, "1"};
                    } catch (Exception e) {
                        logger.error("feedback error:{}", e.getMessage());
                        return null;
                    }
                });
                try {
                    String[] ret = (String[]) future.get();
                    if (ret != null) {
                        if (ret[1].equals("1")) success.add(ret[0]);
                        else history.add(ret[0]);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });
            if (success.size()>0)feedBackMapper.updateFeedbackStatus(success, 1);
            if (history.size()>0)feedBackMapper.updateFeedbackStatus(history, 2);
            return success.size();
        }, FEED_BACK);
    }

    @Override
    public void syncActive() {
        tryWork(r -> {
            Date date = new Date();
            Date before = new Date(date.getTime() - TimeUnit.DAYS.toMillis(1));
            Set<String> imeis = feedBackMapper.getDayImeis(before);

            Long curent = date.getTime();
            Long offset = TimeUnit.MINUTES.toMillis(10);
            List<Map<String, Object>> data;

            // 跨天AB表处理
            if (!format.format(date).equals(format.format(new Date(curent + offset)))) {
                data = feedBackMapper.getThirdActiveLogger(channel,"ActiveLogger");
                data.addAll(feedBackMapper.getThirdActiveLogger(channel, "ActiveLogger_B"));
            }
            else data = feedBackMapper.getThirdActiveLogger(channel,feedBackMapper.getTableName());

            data = data.stream().sorted(Comparator.comparing(o -> ((Date) o.get("activetime")))).collect(
                    Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> String.valueOf(o2.get("imei"))))), ArrayList::new)
            );
            ListIterator<Map<String, Object>> it = data.listIterator();
            while (it.hasNext()) {
                Map<String, Object> map = it.next();
                String imei = String.valueOf(map.get("imei"));
                if (imeis.contains(imei)) {
                    it.remove();
                    continue;
                }
                map.put("imeimd5", DigestUtils.md5DigestAsHex(imei.getBytes()));
                map.put("wifimacmd5", DigestUtils.md5DigestAsHex(String.valueOf(map.get("wifimac")).getBytes()));
            }
            return feedBackMapper.insert("ActiveLogger", data);
        }, SYNC_ACTIVE);

    }

    @Override
    public void clean(JobType type) {
        switch (type) {
            case CLEAN_IMEI:
                tryWork(r -> feedBackMapper.cleanDayImeis(new Date(
                                System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1))),
                        CLEAN_IMEI);
                break;
            case CLEAN_ACTIVE:
                tryWork(r -> {
                    Date before = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
                    Integer to = feedBackMapper.transferActiveToHistory(before);
                    feedBackMapper.deleteActiveThird(before);
                    return to;
                }, CLEAN_ACTIVE);
                break;
            case CLEAN_CLICK:
                tryWork(r -> feedBackMapper.cleanClickLog(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))),
                        CLEAN_CLICK);
                break;
        }

    }

    @Override
    public void stat(JobType type) {
        tryWork(r -> feedBackMapper.activeStaticesDay(format.format(new Date()), ",did"), STAT_DAY);
    }

    private void tryWork(Function<JobType, Integer> consumer, JobType type) {
        String k = String.format("%s#%s", channel, type.getKey());
        try {
            logger.info("start {}", k);
            Long start = System.currentTimeMillis();
            if (mode == 2 && redis.process(jedis -> jedis.setnx(k, "")).equals(0L))
                throw new RuntimeException(type.getKey() + " failed because redis setnx return 0");
            Integer r = consumer.apply(type);
            if (mode == 2) redis.expire(k, type.getExpire().intValue());
            logger.info("finished {} in {}ms,count:{}", k, System.currentTimeMillis() - start, r);
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public enum JobType {
        CLEAN_CLICK("CLEAN_CLICK", TimeUnit.DAYS.toMillis(1) - 60 * 60),
        CLEAN_ACTIVE("CLEAN_ACTIVE", TimeUnit.DAYS.toMillis(1) - 60 * 60),
        CLEAN_IMEI("CLEAN_IMEI", TimeUnit.DAYS.toSeconds(1) - 60 * 5),
        FEED_BACK("FEED_BACK", TimeUnit.MINUTES.toMillis(5) - 60),
        SYNC_ACTIVE("SYNC_ACTIVE", TimeUnit.MINUTES.toMillis(5) - 60),
        STAT_DAY("STAT_DAY", TimeUnit.DAYS.toMillis(1) - 60 * 60);

        private String key;
        private Long expire;

        JobType(String key, Long expire) {
            this.key = key;
            this.expire = expire;
        }

        public String getKey() {
            return key;
        }

        public Long getExpire() {
            return expire;
        }
    }
}
