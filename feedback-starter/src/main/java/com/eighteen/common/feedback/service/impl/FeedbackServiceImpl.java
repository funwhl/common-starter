package com.eighteen.common.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.eighteen.common.feedback.EighteenProperties;
import com.eighteen.common.feedback.dao.*;
import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.*;
import com.eighteen.common.feedback.handler.ActiveHandler;
import com.eighteen.common.feedback.handler.FeedbackHandler;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.utils.HttpClientUtils;
import com.eighteen.common.utils.Page;
import com.eighteen.common.utils.ReflectionUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.LockModeType;
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
import static com.eighteen.common.feedback.entity.QIpuaNewUser.ipuaNewUser;
import static com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType.*;
import static com.eighteen.common.utils.DigestUtils.getMd5Str;


/**
 * @author : wangwei
 * @date : 2019/8/22 17:43
 */
@Service
@Slf4j
@EnableConfigurationProperties(EighteenProperties.class)
public class FeedbackServiceImpl implements FeedbackService, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(FeedbackServiceImpl.class);
    private final EighteenProperties etprop;
    @Autowired(required = false)
    FeedBackMapper feedBackMapper;
    @Autowired
    JPAQueryFactory dsl;
    @Autowired
    FeedbackLogMapper feedbackLogMapper;
    @Autowired
    DayHistoryMapper dayHistoryMapper;
    @Autowired
    ClickLogMapper clickLogMapper;
    @Autowired
    ClickLogHistoryMapper clickLogHistoryMapper;
    @Autowired
    ActiveLoggerHistoryMapper activeLoggerHistoryMapper;
    @Autowired
    ActiveLoggerMapper activeLoggerMapper;
    @Autowired(required = false)
    FeedbackHandler feedbackHandler;
    @Autowired(required = false)
    ActiveHandler activeHandler;
    @Autowired
    IpuaNewUserMapper ipuaNewUserMapper;
    @Autowired(required = false)
    private Redis redis;
    @Value("${spring.application.name}")
    private String appName;
    @Value("#{'${18.feedback.range:}'.split(',')}")
    private List<String> range;
    @Value("#{'${18.feedback.filter:}'.split(',')}")
    private List<String> filter;
    @Value("#{'${18.feedback.ipuafilter:}'.split(',')}")
    private List<String> ipuafilter;
    private ExecutorService executor = new ThreadPoolExecutor(20, 20,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
    private Cache<String, List<DayHistory>> dayCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS).concurrencyLevel(1)
            .build();
    private Cache<String, List<ActiveLogger>> activeLoggerCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1)
            .build();
    private Cache<String, List<String>> errorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1)
            .build();
    private Map<String, JPAQuery<Tuple>> queryMap = new LinkedHashMap<>();


    public FeedbackServiceImpl(EighteenProperties properties) {
        this.etprop = properties;
    }

    @Override
    public void feedback() {
        tryWork(r -> {
            AtomicLong success = new AtomicLong(0);
            List<DayHistory> histories = new ArrayList<>();
            List<FeedbackLog> feedbackLogs = new ArrayList<>();
            List<IpuaNewUser> ipuaNewUsers = new ArrayList<>();
            queryMap.forEach((key, e) -> {
                List<ActiveLogger> tupleList = e.fetch().stream().map(tuple -> tuple.get(activeLogger).setClickLog(tuple.get(clickLog))).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(tupleList)) return;
                ArrayList<ActiveLogger> list = tupleList.stream()
                        // 去重 取lastClick
                        .sorted(Comparator.comparing(o -> o.getClickLog().getClickTime()))
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> {
                                    switch (key) {
                                        case "oaid":
                                            return o2.getOaid() + o2.getCoid() + o2.getNcoid();
                                        case "androidId":
                                            return o2.getAndroidId() + o2.getWifimac() + o2.getCoid() + o2.getNcoid();
                                        case "ipua":
                                            return o2.getIpua() + o2.getCoid() + o2.getNcoid();
                                        default:
                                            return o2.getImei() + o2.getCoid() + o2.getNcoid();
                                    }
                                }))), ArrayList::new)
                        );
                List<DayHistory> dayHistories = getDayCache(key);
                List<ActiveLogger> oldUsers = new ArrayList<>();

                list.forEach(activeLogger -> {
                    String value = ReflectionUtils.getFieldValue(activeLogger, key).toString();
                    DayHistory history = new DayHistory().setNcoid(activeLogger.getNcoid()).setCoid(activeLogger.getCoid()).setWd(key).setValue(value).setCreateTime(new Date());
                    if (dayHistories.contains(history)) {
                        oldUsers.add(activeLogger);
                    } else {
                        if (check(key,activeLogger)) {
                            logger.info("other_field_matched : {}",activeLogger.toString());
                            addDayCache(key, Collections.singletonList(history));
                            histories.add(history);
                            oldUsers.add(activeLogger);
                        }
                    }
                });

                List<String> errorUrls = errorCache.getIfPresent("errors");
                List<ActiveLogger> filter = list.stream()
                        .filter(o -> !oldUsers.contains(o) && (errorUrls == null || !errorUrls.contains(o.getClickLog().getCallbackUrl())))
                        .collect(Collectors.toList());
                List<String> values = filter.stream().map(o -> ReflectionUtils.getFieldValue(o, key).toString()).collect(Collectors.toList());

                if (!CollectionUtils.isEmpty(values)) {
                    List<DayHistory> exist;
                    if (key.equals("ipua")) {
                        exist = dsl.selectFrom(ipuaNewUser).where(ipuaNewUser.ipua.in(values)).fetch()
                                .stream().map(o -> new DayHistory().setCoid(o.getCoid()).setNcoid(o.getNcoid()).setValue(o.getIpua())).collect(Collectors.toList());
                    } else exist = feedBackMapper.listFromStatistics(key, values, null, null);
                    exist.forEach(o -> o.setWd(key));
                    List<DayHistory> finalExist = exist;
                    List<ActiveLogger> newUsers = filter.stream().filter(o -> {
                        String value = ReflectionUtils.getFieldValue(o, key).toString();
                        DayHistory history = new DayHistory().setNcoid(o.getNcoid()).setCoid(o.getCoid()).setWd(key).setValue(value).setCreateTime(new Date());
                        boolean b = finalExist.contains(history);
                        if (b) {
                            addDayCache(key, Collections.singletonList(history));
                            histories.add(history);
                            oldUsers.add(o);
                        }
                        return !b;
                    }).collect(Collectors.toList());

                    newUsers.parallelStream().forEach(a -> {
                        try {
                            Boolean flag;
                            ClickLog c = a.getClickLog();
                            String url = c.getCallbackUrl();
                            if (feedbackHandler != null) {
                                flag = feedbackHandler.handler(c);
                            } else {
                                String ret;
                                ret = HttpClientUtils.get(url + "&event_type=1&event_time=" + System.currentTimeMillis());
                                JSONObject jsonObject = (JSONObject) JSONObject.parse(ret);
                                flag = jsonObject.get("result").equals(1);
                            }
                            if (flag) {
                                FeedbackLog feedbackLog = new FeedbackLog();
                                BeanUtils.copyProperties(c, feedbackLog);
                                feedbackLog.setImei(a.getImei()).setOaid(a.getOaid()).setAndroidId(a.getAndroidId());
                                feedbackLogs.add(feedbackLog.setCreateTime(new Date()).setMid(a.getMid()).setEventType(1).setActiveChannel(a.getChannel()).setActiveTime(a.getActiveTime())
                                        .setMatchField(key).setCoid(a.getCoid()).setNcoid(a.getNcoid()).setTs(c.getTs()));
                                String value = ReflectionUtils.getFieldValue(a, key).toString();
                                if (key.equals("ipua"))ipuaNewUsers.add(new IpuaNewUser().setCoid(a.getCoid()).setNcoid(a.getNcoid()).setIp(a.getIp()).setUa(a.getUa()).setIpua(value).setCreateTime(new Date()));
                                DayHistory history = new DayHistory().setWd(key).setValue(value).setCoid(a.getCoid()).setNcoid(a.getNcoid()).setCreateTime(new Date());
                                if (key.equals("imei")) {
                                    String iimei = a.getIimei();
                                    if (StringUtils.isNotBlank(iimei)) {
                                        List<DayHistory> imeiList = new ArrayList<>();
                                        String[] imeis = iimei.split(",");
                                        DayHistory imeiHistory = new DayHistory();
                                        for (String v : imeis) {
                                            BeanUtils.copyProperties(history, imeiHistory);
                                            if (StringUtils.isNotBlank(v)&&!v.equals(value)) imeiList.add(imeiHistory);
                                        }
                                        if (!CollectionUtils.isEmpty(imeiList)) {
                                            addDayCache(key, imeiList);
                                            histories.addAll(imeiList);
                                        }

                                    }
                                }
                                addDayCache(key, Collections.singletonList(history));
                                success.incrementAndGet();

                                histories.add(history);
                                oldUsers.add(a);
                            } else {
                                if (StringUtils.isNotBlank(url)) {
                                    List<String> errors = errorCache.getIfPresent("errors");
                                    if (errors == null) errorCache.put("errors", Lists.newArrayList(url));
                                    else errors.add(url);
                                }
                            }
                        } catch (Exception e1) {
                            logger.error(e1.getMessage());
                        }
                    });
                }

                oldUsers.stream().collect(Collectors.groupingBy(o -> o.getCoid() + "," + o.getNcoid())).forEach((s, activeLoggers) -> {
                    List<String> collect = activeLoggers.stream().map(o -> ReflectionUtils.getFieldValue(o, key).toString()).collect(Collectors.toList());
                    Page<String> page = Page.create(1, 1000, i -> collect);
                    page.forEach(strings -> {
                        Example example = new Example(ActiveLogger.class);
                        example.createCriteria().andIn(key, strings).andEqualTo("coid", Integer.valueOf(s.split(",")[0])).andEqualTo("ncoid", Integer.valueOf(s.split(",")[1]));
                        activeLoggerMapper.updateByExampleSelective(new ActiveLogger().setStatus(1), example);
                    });
                });
            });

            if (!CollectionUtils.isEmpty(feedbackLogs))
                executor.execute(() -> {
                    Page<FeedbackLog> pageFeed = Page.create(1, 60, i -> feedbackLogs);
                    pageFeed.forEach(o -> feedbackLogMapper.insertList(o));
                });

            if (!CollectionUtils.isEmpty(ipuaNewUsers))
                executor.execute(() -> {
                    Page<IpuaNewUser> pageFeed = Page.create(1, 60, i -> ipuaNewUsers);
                    pageFeed.forEach(o -> ipuaNewUserMapper.insertList(o));
                });

            if (!CollectionUtils.isEmpty(histories))
                executor.execute(() -> {
                    Page<DayHistory> page = Page.create(1, 60, i -> histories);
                    page.forEach(o -> dayHistoryMapper.insertList(o));
                });

            return success.get();
        }, FEED_BACK);
    }

    private Boolean check(String key, ActiveLogger activeLogger) {
        return queryMap.keySet().stream().filter(s -> !s.equals(key)).anyMatch(s -> {
            Object object = ReflectionUtils.getFieldValue(activeLogger, s);
            String fieldValue = object ==null?null: object.toString();
            DayHistory history = new DayHistory().setValue(fieldValue).setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd(s);
            return getDayCache(s).stream().filter(d -> StringUtils.isNotBlank(d.getValue())).anyMatch(o -> o.equals(history));
        });
    }

    @Override
    public void syncActive() {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<String> channel = Lists.newArrayList(etprop.getChannel());
        String types = etprop.getTypes();
        if (StringUtils.isNotBlank(types)) {
            channel = Arrays.asList(types.split(","));
        }
        List<String> finalChannel = channel;
        tryWork(r -> {
            Date date = new Date();
            Long current = date.getTime();
            Long offset = TimeUnit.SECONDS.toMillis(20);
            List<ActiveLogger> data;
            Date maxActiveTime = Optional.ofNullable(dsl.select(activeLogger.activeTime.max()).from(activeLogger).fetchOne()).orElse(new Date(current - TimeUnit.DAYS.toMillis(1)));
            // 跨天AB表处理
            if (!format.format(date).equals(format.format(new Date(current + offset)))) {
                data = feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger", maxActiveTime);
                data.addAll(feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger_B", maxActiveTime));
            } else data = feedBackMapper.getThirdActiveLogger(finalChannel, feedBackMapper.getTableName(), maxActiveTime);

//            data = data.stream().sorted(Comparator.comparing(o -> (o.getActiveTime()))).collect(
//                    Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> o2.getImei() +
//                            o2.getCoid() + o2.getNcoid()))), ArrayList::new)
//            );
            ListIterator<ActiveLogger> it = data.listIterator();
            List<DayHistory> imeiCache = getDayCache("imei");
            List<ActiveLogger> active = activeLoggerCache.getIfPresent("active");
            List<ActiveLogger> iimeiActive = new ArrayList<>();
            while (it.hasNext()) {
                ActiveLogger activeLogger = it.next();
                String imei = activeLogger.getImei();
                if (imeiCache.contains(new DayHistory().setWd("imei").setValue(imei).setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()))
                        || (active != null && active.contains(activeLogger))) {
                    it.remove();
                    continue;
                }
                if (activeHandler != null) activeHandler.handler(activeLogger);
                activeLogger.setImeiMd5(getMd5Str(imei))
                        .setAndroidIdMd5(getMd5Str(activeLogger.getAndroidId()))
                        .setOaidMd5(getMd5Str(activeLogger.getOaid()))
                        .setWifimacMd5(getMd5Str(activeLogger.getWifimac()))
                        .setIpua(getMd5Str(activeLogger.getIp() + "#" + activeLogger.getUa()))
                        .setCreateTime(new Date()).setStatus(0);
                String iimei = activeLogger.getIimei();
                if (etprop.getMultipleImei()&&StringUtils.isNotBlank(iimei)) {
                    String[] imeis = iimei.split(",");
                    for (int i = 0; i < imeis.length; i++) {
                        String placeholder = "imei#" + (i + 1);
                        String currentImei = imeis[i];
                        if (StringUtils.isNotBlank(currentImei)&&!currentImei.equals(imei)) {
                            ActiveLogger e = new ActiveLogger();
                            BeanUtils.copyProperties(activeLogger, e);
                            e.setIpua(placeholder);
                            e.setOaidMd5(placeholder);
                            e.setAndroidIdMd5(placeholder);
                            e.setOaid(placeholder);
                            e.setImei(currentImei);
                            e.setImeiMd5(getMd5Str(currentImei));
                            iimeiActive.add(e);
                        }
                    }
                }
            }
            if (!CollectionUtils.isEmpty(iimeiActive)) data.addAll(iimeiActive);
            if (!CollectionUtils.isEmpty(data)) {
                Page<ActiveLogger> page = Page.create(1, 60, i -> data);
                page.forEach(activeLoggers -> activeLoggerMapper.insertList(activeLoggers));
                activeLoggerCache.invalidate("active");
                activeLoggerCache.put("active", data);
            }
            return data.size();
        }, SYNC_ACTIVE);

    }

    @Override
    @Transactional
    public void clean(JobType type) {
        Integer offset = etprop.getOffset();
        switch (type) {
            case CLEAN_IMEI:
                Long current = System.currentTimeMillis();
                tryWork(r -> dsl.delete(dayHistory).setLockMode(LockModeType.NONE).where(dayHistory.createTime.before(new Date(
                                current - TimeUnit.DAYS.toMillis(offset + 1)))).execute(),
                        CLEAN_IMEI);
                if (etprop.getRetention()) {
                    tryWork(r -> feedBackMapper.cleanDayLCImeis(new Date(
                                    current - TimeUnit.DAYS.toMillis(offset))),
                            CLEAN_LC_IMEI);
                }
                clearCache(null);
                break;
            case CLEAN_ACTIVE:
                tryWork(r -> {
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
                            .where(activeLogger.createTime.before(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset)))).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;
                    cleanActiveLogger(activeLoggers);
                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE);
                break;
            case CLEAN_ACTIVE_HISTORY:
                tryWork(r -> {
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
                            .where(activeLogger.status.ne(0).and(activeLogger.createTime
                                    .before(JPAExpressions.select(activeLogger.createTime.max()).from(activeLogger)))).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;

                    cleanActiveLogger(activeLoggers);
                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE_HISTORY);
                break;
            case CLEAN_CLICK:
                tryWork(r -> {
                            List<ClickLog> clickLogs = dsl.selectFrom(clickLog).setLockMode(LockModeType.NONE)
                                    .where(clickLog.createTime.before(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset)))).fetch();
                            if (CollectionUtils.isEmpty(clickLogs)) return 0;

                            List<ClickLogHistory> list = clickLogs.stream().map(clickLog1 -> {
                                ClickLogHistory clickLogHistory = new ClickLogHistory();
                                BeanUtils.copyProperties(clickLog1, clickLogHistory);
                                clickLogHistory.setId(null);
                                return clickLogHistory;
                            }).collect(Collectors.toList());

                            Page<ClickLogHistory> pageList = Page.create(1, 60, i -> list);
                            pageList.forEach(data -> clickLogHistoryMapper.insertList(data));

                            List<Long> ids = clickLogs.stream().map(ClickLog::getId).collect(Collectors.toList());
                            Page<Long> pageIds = Page.create(1, 1000, i -> ids);
                            pageIds.forEach(o -> dsl.delete(clickLog).setLockMode(LockModeType.NONE).where(clickLog.id.in(o)).execute());
                            return (long) clickLogs.size();
                        },
                        CLEAN_CLICK);
                break;
        }

    }

    @Override
    public void clearCache(Integer offset) {
        dayCache.invalidateAll();
        if (redis != null) {
            if (offset == null) {
                queryMap.keySet().forEach(s -> {
                    Long zexpire = redis.del(getDayCacheRedisKey(s));
                    logger.info("clear_cache:{}",zexpire);
                });
            } else {
                queryMap.keySet().forEach(s -> redis.zexpire(getDayCacheRedisKey(s), (double) 0, (double) (System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1))));
            }
        }
    }

    private void cleanActiveLogger(List<ActiveLogger> activeLoggers) {
        List<ActiveLoggerHistory> histories = activeLoggers.stream().map(a -> {
            ActiveLoggerHistory activeLoggerHistory = new ActiveLoggerHistory();
            BeanUtils.copyProperties(a, activeLoggerHistory);
            activeLoggerHistory.setId(null);
            return activeLoggerHistory;
        }).collect(Collectors.toList());

        Page<ActiveLoggerHistory> page = Page.create(1, 60, i -> histories);
        page.forEach(list -> activeLoggerHistoryMapper.insertList(list));

        List<Long> ids = activeLoggers.stream().map(ActiveLogger::getId).collect(Collectors.toList());
        Page<Long> idPage = Page.create(1, 100, i -> ids);
        idPage.forEach(list -> dsl.delete(activeLogger).setLockMode(LockModeType.NONE).where(activeLogger.id.in(list)).execute());
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

                        feedbackLogMapper.insertList(Lists.newArrayList(new FeedbackLog()
                                .setAndroidId(thirdRetentionLog.getAndroidId())
                                .setCallbackUrl(url).setCreateTime(new Date()).setChannel(thirdRetentionLog.getChannel()).setEventType(7)
                                .setIp(thirdRetentionLog.getIp()).setImei(thirdRetentionLog.getImei()).setMac(thirdRetentionLog.getMac())
                                .setMatchField("imei").setOaid(thirdRetentionLog.getOaid()).setTs(System.currentTimeMillis())));
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
        String k = String.format("%s#%s", etprop.getChannel(), type.getKey());
        Integer mode = etprop.getMode();
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
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getDayCacheRedisKey(String key) {
        return appName + "#dayHistory#" + key;
    }

    @Override
    public void addDayCache(String key, List<DayHistory> dayHistories) {
        try {
            if (CollectionUtils.isEmpty(dayHistories)) return;
            if (redis != null) {
                String redisKey = getDayCacheRedisKey(key);
                Map<String, Double> map = new HashMap<>();
                dayHistories.forEach(dayHistory -> map.put(String.format("%d##%d##%s", dayHistory.getCoid(), dayHistory.getNcoid(), dayHistory.getValue()), (double) dayHistory.getCreateTime().getTime()));
                if (map.size() > 0) redis.process(j -> {
                    Long zadd = j.zadd(redisKey, map);
                    if (zadd <= 0) logger.error("add_redis_error key:{},data:{}",redisKey,map);
                    return zadd;
                });
            } else {
                List<DayHistory> list = dayCache.getIfPresent(key);
                if (CollectionUtils.isEmpty(list)) {
                    dayCache.put(key, dayHistories);
                } else list.addAll(dayHistories);
            }
        } catch (Exception e) {
            redis = null;
            logger.error("add_cache_error"+e.getMessage());
        }
    }

    @Override
    public List<DayHistory> getDayCache(String key) {
        List<DayHistory> dayHistories;
        Integer offset = etprop.getOffset();
        try {
            if (redis != null) {
                String redisKey = getDayCacheRedisKey(key);
                long end = System.currentTimeMillis();
                dayHistories = redis.zrange(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) end + TimeUnit.HOURS.toMillis(8)).stream().map(s -> {
                    String[] split = s.split("##");
                    String value = split.length < 3 ? "" : split[2];
                    return new DayHistory().setWd(key).setCoid(split[0] == null ? null : Integer.valueOf(split[0]))
                            .setNcoid(split[1] == null ? null : Integer.valueOf(split[1])).setValue(value);
                }).collect(Collectors.toList());
                logger.info("get_redis:{}",dayHistories==null?"0":dayHistories.size());
            } else {
                dayHistories = dayCache.getIfPresent(key);
            }
            if (CollectionUtils.isEmpty(dayHistories)) {
                Example example = new Example(DayHistory.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("wd", key);
                criteria.andGreaterThan("createTime", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1)));
                dayHistories = dayHistoryMapper.selectByExample(example);
                addDayCache(key, dayHistories);
            }
        } catch (Exception e) {
            logger.error("redis_cache_error: {}",e.getMessage());
            Example example = new Example(DayHistory.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("wd", key);
            criteria.andGreaterThan("createTime", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1)));
            dayHistories = dayHistoryMapper.selectByExample(example);
            e.printStackTrace();
        }
        return dayHistories;
    }

    @Override
    public void afterPropertiesSet() {
        BooleanExpression imeiBe = activeLogger.imeiMd5.eq(clickLog.imeiMd5);
        BooleanExpression oaidBe = activeLogger.oaidMd5.eq(clickLog.oaidMd5);
        BooleanExpression androidIdBe = activeLogger.androidIdMd5.eq(clickLog.androidIdMd5);
        if (etprop.getMacAttributed()) androidIdBe =androidIdBe.and(activeLogger.wifimacMd5.eq(clickLog.mac));
        BooleanExpression ipuaBe = activeLogger.ipua.eq(clickLog.ipua);
        Map<String, BooleanExpression> wd = new LinkedHashMap<>();

        wd.put("imei", imeiBe.and(activeLogger.imeiMd5.isNotNull()).and(activeLogger.imeiMd5.isNotEmpty()));
        wd.put("oaid", oaidBe.and(imeiBe.not()).and(activeLogger.oaidMd5.isNotNull()).and(activeLogger.oaidMd5.isNotEmpty()));
        wd.put("androidId", androidIdBe.and(imeiBe.not()).and(oaidBe.not()).and(activeLogger.androidIdMd5.isNotNull()).and(activeLogger.androidIdMd5.isNotEmpty()));
        if (etprop.getIpAttributed()) {
            BooleanExpression booleanExpression = ipuaBe.and(imeiBe.not()).and(oaidBe.not()).and(androidIdBe.not())
                    .and(activeLogger.ipua.isNotEmpty());
            if (!CollectionUtils.isEmpty(ipuafilter) && !(ipuafilter.size() == 1 && StringUtils.isBlank(ipuafilter.get(0)))) {
                booleanExpression = booleanExpression.and(activeLogger.channel.in(ipuafilter));
            }
            wd.put("ipua", booleanExpression);
        }

        wd.forEach((s, e) -> {
            if (!CollectionUtils.isEmpty(range) && !(range.size() == 1 && StringUtils.isBlank(range.get(0))))
                e = e.and(activeLogger.channel.in(range));
            if (!CollectionUtils.isEmpty(filter) && !(filter.size() == 1 && StringUtils.isBlank(filter.get(0))))
                e = e.and(activeLogger.channel.notIn(filter));
            if (etprop.getDatetimeAttributed())
                e = e.and(activeLogger.activeTime.after(clickLog.clickTime));
            if (etprop.getMatchMinuteOffset() > 0)
                e = e.and(Expressions.
                        booleanTemplate("abs(datediff(minute,{0},{1})) >= {2}", clickLog.clickTime, activeLogger.activeTime, 30));
            queryMap.put(s, dsl.select(activeLogger, clickLog).from(activeLogger).setLockMode(LockModeType.NONE).innerJoin(clickLog).on(e.and(activeLogger.status.eq(0))).limit(1000L));
        });
        clearCache(null);
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
