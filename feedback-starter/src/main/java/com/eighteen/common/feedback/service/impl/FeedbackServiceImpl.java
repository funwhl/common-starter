package com.eighteen.common.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.FeedBackMapper;
import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.ActiveLogger;
import com.eighteen.common.feedback.entity.ClickLog;
import com.eighteen.common.feedback.entity.DayHistory;
import com.eighteen.common.feedback.entity.FeedbackLog;
import com.eighteen.common.feedback.entity.dao2.ActiveLoggerDao;
import com.eighteen.common.feedback.entity.dao2.DayHistoryDao;
import com.eighteen.common.feedback.entity.dao2.FeedbackLogDao;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.utils.HttpClientUtils;
import com.eighteen.common.utils.ReflectionUtils;
import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.BooleanOperation;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.eighteen.common.feedback.entity.QActiveLogger.activeLogger;
import static com.eighteen.common.feedback.entity.QClickLog.clickLog;
import static com.eighteen.common.feedback.entity.QDayHistory.dayHistory;
import static com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType.*;


/**
 * @author : wangwei
 * @date : 2019/8/22 17:43
 */
@Service
@Slf4j
public class FeedbackServiceImpl implements FeedbackService {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackServiceImpl.class);
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    @Autowired
    JPAQueryFactory dsl;
    @Autowired
    FeedbackLogDao feedbackLogDao;
    @Autowired
    DayHistoryDao dayHistoryDao;
    @Autowired
    ActiveLoggerDao activeLoggerDao;

    @Autowired(required = false)
    private Redis redis;
    @Value("${18.feedback.channel}")
    private String channel;
    @Value("${18.feedback.mode}")
    private int mode;
    @Value("${spring.application.name}")
    private String appName;
    private ExecutorService executor = new ThreadPoolExecutor(8, 8,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());

    @Override
    public void feedback() {
        tryWork(r -> {
            BooleanExpression imeiBe = activeLogger.imei.eq(clickLog.imei);
            BooleanExpression oaidBe = activeLogger.oaid.eq(clickLog.oaid);
            BooleanExpression androidIdBe = activeLogger.androidId.eq(clickLog.androidId);
            BooleanExpression wifiMacBe = activeLogger.wifimac.in(clickLog.mac, clickLog.mac2);
            AtomicInteger success = new AtomicInteger(0);

            ArrayList<BooleanExpression> wd = Lists.newArrayList(imeiBe
                    , oaidBe.and(imeiBe.not())
                    , androidIdBe.and(imeiBe.not()).and(oaidBe.not())
                    , wifiMacBe.and(imeiBe.not()).and(oaidBe.not()).and(androidIdBe.not()));
            List<DayHistory> dayHistories = dsl.selectFrom(dayHistory).where(dayHistory.createTime.lt(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))).fetch();
            List<DayHistory> histories = new ArrayList<>();
            wd.forEach(e -> {
                String type = ((BooleanOperation) e).getArg(0).toString().replace("activeLogger.", "");
                dsl.select(activeLogger, clickLog).from(activeLogger).innerJoin(clickLog).on(e).limit(1000L).fetch().stream()
                        .sorted(Comparator.comparing(o -> o.get(activeLogger).getActiveTime()))
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> ReflectionUtils.getFieldValue(o2.get(activeLogger), type).toString()))), ArrayList::new)
                        ).parallelStream().forEach(tuple -> {
                    ActiveLogger a = tuple.get(activeLogger);
                    ClickLog c = tuple.get(clickLog);
                    DayHistory history = new DayHistory().setCoid(a.getCoid()).setNcoid(a.getNcoid()).setWd(type).setValue(ReflectionUtils.getFieldValue(a, type).toString());
                    if (dayHistories.contains(history)) return;
                    if (feedBackMapper.countFromStatistics(a.getImei(), a.getCoid(), a.getNcoid()) <= 0) {
                        String url = c.getCallbackUrl() + "&event_type=1&event_time=" + System.currentTimeMillis();
                        String ret;
                        try {
                            ret = HttpClientUtils.get(url);
                            JSONObject jsonObject = (JSONObject) JSONObject.parse(ret);
                            if (jsonObject.get("result").equals(1)) {
                                executor.submit(() -> {
                                    feedbackLogDao.save(new FeedbackLog()
                                            .setAid(c.getAid()).setCid(c.getCid()).setAndroidId(c.getAndroidId())
                                            .setCallbackUrl(c.getCallbackUrl()).setCreateTime(new Date()).setChannel(c.getChannel()).setEventType(1)
                                            .setIp(a.getIp()).setImei(a.getImei()).setMac(c.getMac()).setMatchField(type).setOaid(c.getOaid()));
                                });
                                history.setStatus(1);
                                success.incrementAndGet();
                                histories.add(history);
                            }
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    } else {
                        history.setStatus(2);
                        histories.add(history);
                    }
                });
            });
            executor.submit(() -> dayHistoryDao.saveAll(histories));
            return success.get();
        }, FEED_BACK);
    }

    @Override
    public void syncActive() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        tryWork(r -> {
            Date date = new Date();
            List<DayHistory> histories = dsl.selectFrom(dayHistory)
                    .where(dayHistory.createTime.lt(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)))
                            .and(dayHistory.wd.eq("imei"))).fetch();

            Long curent = date.getTime();
            Long offset = TimeUnit.MINUTES.toMillis(10);
            List<ActiveLogger> data;

            // 跨天AB表处理
            if (!format.format(date).equals(format.format(new Date(curent + offset)))) {
                data = feedBackMapper.getThirdActiveLogger(channel, "ActiveLogger");
                data.addAll(feedBackMapper.getThirdActiveLogger(channel, "ActiveLogger_B"));
            } else data = feedBackMapper.getThirdActiveLogger(channel, feedBackMapper.getTableName());

            data = data.stream().sorted(Comparator.comparing(o -> (o.getActiveTime()))).collect(
                    Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> o2.getImei() +
                            o2.getCoid() + o2.getNcoid()))), ArrayList::new)
            );
            ListIterator<ActiveLogger> it = data.listIterator();
            while (it.hasNext()) {
                ActiveLogger activeLogger = it.next();
                String imei = activeLogger.getImei();
                if (histories.contains(new DayHistory().setWd("imei").setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()))) {
                    it.remove();
                    continue;
                }
                activeLogger.setImeiMd5(DigestUtils.md5DigestAsHex(imei.getBytes()));
                activeLogger.setWifimacMd5(DigestUtils.md5DigestAsHex(activeLogger.getWifimac().getBytes()));
            }

            activeLoggerDao.saveAll(data);
            return data.size();
        }, SYNC_ACTIVE);

    }

    @Override
    public void clean(JobType type) {
        switch (type) {
            case CLEAN_IMEI:
                Long current = System.currentTimeMillis();
                tryWork(r -> feedBackMapper.cleanDayImeis(new Date(
                                current - TimeUnit.DAYS.toMillis(1))),
                        CLEAN_IMEI);
                tryWork(r -> feedBackMapper.cleanDayLCImeis(new Date(
                                current - TimeUnit.DAYS.toMillis(2))),
                        CLEAN_LC_IMEI);
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
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        tryWork(r -> feedBackMapper.activeStaticesDay(format.format(new Date()), ",did"), STAT_DAY);
    }

    @Override
    public void secondStay(JobType type) {
        tryWork(jobType -> {
            List<ThirdRetentionLog> list = feedBackMapper.getSecondStay();
            Map<String, ThirdRetentionLog> mapRetention = new HashMap<>();
            list.forEach(e -> mapRetention.put(e.getImei(), e));
            int success = 0;
            for (ThirdRetentionLog thirdRetentionLog : mapRetention.values()) {
                String url = thirdRetentionLog.getCallBack() + "&event_type=7&event_time=" + System.currentTimeMillis();
                try {
                    String ret = HttpClientUtils.get(url);
                    JSONObject jsonObject = (JSONObject) JSONObject.parse(ret);
                    if (jsonObject.get("result").equals(1)) {
                        feedbackLogDao.save(new FeedbackLog()
                                .setAid(thirdRetentionLog.getAid()).setCid(thirdRetentionLog.getCid()).setAndroidId(thirdRetentionLog.getAndroidId())
                                .setCallbackUrl(url).setCreateTime(new Date()).setChannel(thirdRetentionLog.getChannel()).setEventType(7)
                                .setIp(thirdRetentionLog.getIp()).setImei(thirdRetentionLog.getImei()).setMac(thirdRetentionLog.getMac())
                                .setMatchField("cl").setOaid(thirdRetentionLog.getOaid()));
                        success += success;
                        feedBackMapper.insertDayLiucunImei(thirdRetentionLog.getImei(), thirdRetentionLog.getImeimd5());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
            return success;
        }, RETENTION);
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
        CLEAN_LC_IMEI("CLEAN_LC_IMEI", TimeUnit.DAYS.toSeconds(2) - 60 * 5),
        FEED_BACK("FEED_BACK", TimeUnit.MINUTES.toMillis(5) - 60),
        SYNC_ACTIVE("SYNC_ACTIVE", TimeUnit.MINUTES.toMillis(5) - 60),
        STAT_DAY("STAT_DAY", TimeUnit.DAYS.toMillis(1) - 60 * 60),
        RETENTION("RETENTION", TimeUnit.DAYS.toMillis(10) - 60);

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
