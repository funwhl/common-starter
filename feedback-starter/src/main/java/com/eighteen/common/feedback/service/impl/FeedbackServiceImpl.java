package com.eighteen.common.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.dao.FeedBackMapper;
import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.*;
import com.eighteen.common.feedback.entity.dao2.*;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.utils.HttpClientUtils;
import com.eighteen.common.utils.ReflectionUtils;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
    @Autowired
    ClickLogDao clickLogDao;
    @Autowired
    ClickLogHistoryDao clickLogHistoryDao;
    @Autowired
    ActiveLoggerHistoryDao activeLoggerHistoryDao;
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
    @Transactional
    public void feedback() {
        tryWork(r -> {
            BooleanExpression imeiBe = activeLogger.imei.eq(clickLog.imei);
            BooleanExpression oaidBe = activeLogger.oaid.eq(clickLog.oaid);
            BooleanExpression androidIdBe = activeLogger.androidId.eq(clickLog.androidId);
//            BooleanExpression wifiMacBe = activeLogger.wifimac.eq(clickLog.mac);
            AtomicLong success = new AtomicLong(0);

            Map<String, BooleanExpression> wd = new HashMap<>();
            wd.put("imei", imeiBe.and(activeLogger.imei.isNotNull()));
            wd.put("oaid", oaidBe.and(imeiBe.not()).and(activeLogger.oaid.isNotNull()));
            wd.put("androidId", androidIdBe.and(imeiBe.not()).and(oaidBe.not()).and(activeLogger.androidId.isNotNull()));
//            wd.put("wifimac", wifiMacBe.and(imeiBe.not()).and(oaidBe.not()).and(androidIdBe.not()).and(activeLogger.wifimac.isNotNull()));

            List<DayHistory> dayHistories = dsl.selectFrom(dayHistory).where(dayHistory.createTime.after(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))).fetch();
            List<DayHistory> histories = new ArrayList<>();
            wd.forEach((key, e) -> {
                String type = key;
//                if (type.equals("mac")) type = "wifimac";
                String finalType = type;
                ArrayList<Tuple> list = dsl.select(activeLogger, clickLog).from(activeLogger).innerJoin(clickLog).on(e.and(activeLogger.status.eq(0))).limit(1000L).fetch().stream()
                        .sorted(Comparator.comparing(o -> o.get(activeLogger).getActiveTime()))
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> {
                                    Object fieldValue = ReflectionUtils.getFieldValue(o2.get(activeLogger), finalType);
                                    return fieldValue == null ? "" : fieldValue.toString();
                                }))), ArrayList::new)
                        );
                list.forEach(tuple -> {
                    try {
                        ActiveLogger a = tuple.get(activeLogger);
                        ClickLog c = tuple.get(clickLog);
                        String value = ReflectionUtils.getFieldValue(a, finalType).toString();
                        DayHistory history = new DayHistory().setWd(finalType).setValue(value).setCreateTime(new Date());
                        if (a.getCoid() != null) history.setCoid(a.getCoid());
                        if (a.getNcoid() != null) history.setNcoid(a.getNcoid());
                        if (!dayHistories.contains(history)) {
                            try {
                                //feedBackMapper.countFromStatistics(finalType, value, a.getCoid(), a.getNcoid()) <= 0
                                if (true) {
//                                    String url = c.getCallbackUrl() + "&event_type=1&event_time=" + System.currentTimeMillis();
//                                    String ret;
//                                    ret = HttpClientUtils.get(url);
//                                    JSONObject jsonObject = (JSONObject) JSONObject.parse(ret);
                                    //jsonObject.get("result").equals(1)
                                    if (true) {
                                        feedbackLogDao.save(new FeedbackLog()
                                                .setAid(c.getAid()).setCid(c.getCid()).setAndroidId(c.getAndroidId())
                                                .setCallbackUrl(c.getCallbackUrl()).setCreateTime(new Date()).setChannel(c.getChannel()).setEventType(1)
                                                .setIp(a.getIp()).setImei(a.getImei()).setMac(c.getMac()).setMatchField(finalType).setOaid(c.getOaid()));
                                        history.setStatus(1);

                                        BooleanExpression eq = activeLogger.imei.eq(value);
                                        if (finalType.equals("oaid")) eq = activeLogger.oaid.eq(value);
                                        if (finalType.equals("androidId")) eq = activeLogger.androidId.eq(value);
                                        if (finalType.equals("wifimac"))
                                            eq = activeLogger.wifimac.in(c.getMac(), c.getMac2());
                                        if (a.getCoid() != null) eq.and(activeLogger.coid.eq(a.getCoid()));
                                        if (a.getNcoid() != null) eq.and(activeLogger.ncoid.eq(a.getNcoid()));
                                        dsl.update(activeLogger).set(activeLogger.status, 1)
                                                .where(eq).execute();
                                        success.incrementAndGet();
                                        histories.add(history);
                                    }

                                } else {
                                    histories.add(history);
                                }
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        } else {
                            BooleanExpression eq = activeLogger.imei.eq(value);
                            if (finalType.equals("oaid")) eq = activeLogger.oaid.eq(value);
                            if (finalType.equals("androidId")) eq = activeLogger.androidId.eq(value);
                            if (finalType.equals("wifimac")) eq = activeLogger.wifimac.in(c.getMac(), c.getMac2());
                            if (a.getCoid() != null) eq.and(activeLogger.coid.eq(a.getCoid()));
                            if (a.getNcoid() != null) eq.and(activeLogger.ncoid.eq(a.getNcoid()));

                            dsl.update(activeLogger).set(activeLogger.status, 2).where(eq).execute();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                });
            });
            dayHistoryDao.saveAll(histories);
            return success.get();
        }, FEED_BACK);
    }

    @Override
    public void syncActive() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        tryWork(r -> {
            Date date = new Date();
            List<DayHistory> histories = dsl.selectFrom(dayHistory)
                    .where(dayHistory.createTime.after(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))
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
                if (imei!=null)activeLogger.setImeiMd5(DigestUtils.md5DigestAsHex(imei.getBytes()));
                if (activeLogger.getWifimac()!=null)activeLogger.setWifimacMd5(DigestUtils.md5DigestAsHex(activeLogger.getWifimac().getBytes()));
            }

            activeLoggerDao.saveAll(data);
            return data.size();
        }, SYNC_ACTIVE);

    }

    @Override
    @Transactional
    public void clean(JobType type) {
        switch (type) {
            case CLEAN_IMEI:
                Long current = System.currentTimeMillis();
                tryWork(r -> dsl.delete(dayHistory).where(dayHistory.createTime.before(new Date(
                                current - TimeUnit.DAYS.toMillis(1)))).execute(),
                        CLEAN_IMEI);
                tryWork(r -> feedBackMapper.cleanDayLCImeis(new Date(
                                current - TimeUnit.DAYS.toMillis(2))),
                        CLEAN_LC_IMEI);
                break;
            case CLEAN_ACTIVE:
//                tryWork(r -> {
//                    Date before = new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2));
//                    Long to = feedBackMapper.transferActiveToHistory(before);
//                    feedBackMapper.deleteActiveThird(before);
//                    return to;
//                }, CLEAN_ACTIVE);
                tryWork(r -> {
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger)
                            .where(activeLogger.createTime.before(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)))).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0l;
                    activeLoggerHistoryDao.saveAll(activeLoggers.stream().map(a -> {
                        ActiveLoggerHistory activeLoggerHistory = new ActiveLoggerHistory();
                        BeanUtils.copyProperties(a, activeLoggerHistory);
                        activeLoggerHistory.setId(null);
                        return activeLoggerHistory;
                    }).collect(Collectors.toList()));
                    activeLoggerDao.deleteAll(activeLoggers);
                    return (long)activeLoggers.size();
                }, CLEAN_ACTIVE);
                break;
            case CLEAN_ACTIVE_HISTORY:
                tryWork(r -> {
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger)
                            .where(activeLogger.status.ne(0)).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;
                    activeLoggerHistoryDao.saveAll(activeLoggers.stream().map(a -> {
                        ActiveLoggerHistory activeLoggerHistory = new ActiveLoggerHistory();
                        BeanUtils.copyProperties(a, activeLoggerHistory);
                        activeLoggerHistory.setId(null);
                        return activeLoggerHistory;
                    }).collect(Collectors.toList()));
                    activeLoggerDao.deleteAll(activeLoggers);
                    return (long)activeLoggers.size();
                }, CLEAN_ACTIVE_HISTORY);
                break;
            case CLEAN_CLICK:
//                tryWork(r -> feedBackMapper.cleanClickLog(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7))),
//                        CLEAN_CLICK);
                tryWork(r -> {
                            List<ClickLog> clickLogs = dsl.selectFrom(clickLog)
                                    .where(clickLog.clickTime.after(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)))).fetch();
                            if (CollectionUtils.isEmpty(clickLogs)) return 0;
                            clickLogHistoryDao.saveAll(clickLogs.stream().map(clickLog1 -> {
                                ClickLogHistory clickLogHistory = new ClickLogHistory();
                                BeanUtils.copyProperties(clickLog1, clickLogHistory);
                                return clickLogHistory;
                            }).collect(Collectors.toList()));
                            clickLogDao.deleteAll(clickLogs);
                            return (long)clickLogs.size();
                        },
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
                                .setMatchField("imei").setOaid(thirdRetentionLog.getOaid()));
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

    private void tryWork(Function<JobType, Object> consumer, JobType type) {
        String k = String.format("%s#%s", channel, type.getKey());
        try {
            logger.info("start {}", k);
            Long start = System.currentTimeMillis();
            if (mode == 2 && redis.process(jedis -> jedis.setnx(k, "")).equals(0L))
                throw new RuntimeException(type.getKey() + " failed because redis setnx return 0");
            Object r = consumer.apply(type);
            if (mode == 2) redis.expire(k, type.getExpire().intValue());
            logger.info("finished {} in {}ms,count:{}", k, System.currentTimeMillis() - start, r.toString());
        } catch (RuntimeException e) {
            logger.info(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public enum JobType {
        CLEAN_CLICK("CLEAN_CLICK", TimeUnit.DAYS.toMillis(1) - 60 * 60),
        CLEAN_ACTIVE("CLEAN_ACTIVE", TimeUnit.DAYS.toMillis(1) - 60 * 60),
        CLEAN_ACTIVE_HISTORY("CLEAN_ACTIVE_HISTORY", TimeUnit.DAYS.toMillis(1) - 60 * 60),
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
