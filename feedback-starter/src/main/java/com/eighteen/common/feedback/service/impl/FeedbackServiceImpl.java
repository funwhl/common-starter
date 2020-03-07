package com.eighteen.common.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.eighteen.common.distribution.DistributedLock;
import com.eighteen.common.distribution.ZooKeeperConnector;
import com.eighteen.common.feedback.EighteenProperties;
import com.eighteen.common.feedback.dao.*;
import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.*;
import com.eighteen.common.feedback.handler.ActiveHandler;
import com.eighteen.common.feedback.handler.FeedbackHandler;
import com.eighteen.common.feedback.service.FeedbackService;
import com.eighteen.common.spring.boot.autoconfigure.cache.redis.Redis;
import com.eighteen.common.utils.FsService;
import com.eighteen.common.utils.HttpClientUtils;
import com.eighteen.common.utils.Page;
import com.eighteen.common.utils.ReflectionUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
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
import static com.eighteen.common.feedback.entity.QIpuaNewUser.ipuaNewUser;
import static com.eighteen.common.feedback.service.impl.FeedbackServiceImpl.JobType.*;
import static com.eighteen.common.utils.DigestUtils.getMd5StrWithPlaceholder;


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
    RestTemplate restTemplate;
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
    @Autowired
    WebLogMapper webLogMapper;
    @Autowired
    FsService fsService;
//    List<String> sds = Lists.newArrayList("0,1", "2,3,4,5", "6,7,8,9");
    List<String> sds = new ArrayList<>();
    @Autowired(required = false)
    private Redis redis;
    @Value("${spring.application.name}")
    private String appName;
    @Value("#{'${18.feedback.range:}'.split(',')}")
    private List<String> range;
    @Value("#{'${18.feedback.filter:}'.split(',')}")
    private List<String> filterChannels;
    @Value("#{'${18.feedback.ipuaChannels:}'.split(',')}")
    private List<String> ipuaChannels;
    private List<String> filters = Lists.newArrayList("null", "Null", "NULL", "{{IMEI}}", "{{ANDDROID_ID}}", "{{OAID}}", "", "__IMEI__", "__OAID__");
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
    private Map<String, BooleanExpression> queryMap = new LinkedHashMap<>(4);

    public FeedbackServiceImpl(EighteenProperties properties) {
        this.etprop = properties;
    }

    @Autowired
    ZooKeeperConnector zooKeeperConnector;
    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void feedback(ShardingContext sc) {
        tryWork(r -> {
            AtomicLong success = new AtomicLong(0);
            List<DayHistory> histories = new ArrayList<>();
            List<FeedbackLog> feedbackLogs = new ArrayList<>();
            List<IpuaNewUser> ipuaNewUsers = new ArrayList<>();
            String[] sd = sds.get(sc.getShardingItem()).split(",");
            Date date = dsl.select(activeLogger.activeTime.max()).from(activeLogger).fetchOne();
            Map<String, List<ActiveLogger>> map = queryMap.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, e ->
                    dsl.select(activeLogger, clickLog).from(activeLogger).setLockMode(LockModeType.NONE).innerJoin(clickLog).on(e.getValue())
                            .where(activeLogger.status.eq(0)
//                                .and(activeLogger.sd.eq(sc == null ? 0 : sc.getShardingItem()))
                            .and(activeLogger.activeTime.goe(new Date(date.getTime() - TimeUnit.MINUTES.toMillis(etprop.getActiveMinuteOffset())))
                            )
//                        .and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).between(sd[0],sd[1]))
                        .and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).goe(sd[0]).and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).loe(sd[1])))
            ).limit(Long.valueOf(etprop.getPreFetch())).fetch().stream().map(tuple -> tuple.get(activeLogger).setClickLog(tuple.get(clickLog))).collect(Collectors.toList())));

            map.forEach((key, e) -> {
                StopWatch watch = StopWatch.createStarted();
                logger.info("{}start {},{},{}",sd, key, watch.toString());
                lock.lock(FEED_BACK.name(),FEED_BACK.name(),() -> {
                if (CollectionUtils.isEmpty(e)) return;
                List<ActiveLogger> list = e.stream()
                        .sorted((o1, o2) -> o2.getClickLog().getClickTime().compareTo(o1.getClickLog().getClickTime()))
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
                List<ActiveLogger> oldUsers = new ArrayList<>();

                 list.forEach(activeLogger -> {
                        String value = ReflectionUtils.getFieldValue(activeLogger, key).toString();
                        DayHistory history = new DayHistory().setNcoid(activeLogger.getNcoid()).setCoid(activeLogger.getCoid()).setWd(key).setValue(value).setCreateTime(new Date());
                        if (countHistory(history)) {
                            oldUsers.add(activeLogger);
                        } else if (check(key, activeLogger)) {
                            logger.info("other_field_matched : key:{},value:{}#{}#{} ,{}", key, value, activeLogger.getCoid(), activeLogger.getNcoid(), activeLogger.toString());
                            addDayCache(key, Collections.singletonList(history));
                            histories.add(history);
                            oldUsers.add(activeLogger);
                        }
                    });
                    List<String> retErrors = errorCache.getIfPresent("errors");
                    List<ActiveLogger> filter = list.stream()
                            .filter(o -> !oldUsers.contains(o) && (retErrors == null || !retErrors.contains(o.getClickLog().getCallbackUrl())))
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

                        newUsers.forEach(a -> {
                            try {
                                ClickLog c = a.getClickLog();
                                Boolean flag;
                                if (feedbackHandler != null) flag = feedbackHandler.handler(c);
                                else
                                    flag = restTemplate.getForEntity(c.getCallbackUrl(), String.class).getStatusCode().value() == 200;
                                if (flag) {
                                    FeedbackLog feedbackLog = new FeedbackLog();
                                    BeanUtils.copyProperties(c, feedbackLog);
                                    feedbackLog.setImei(a.getImei()).setOaid(a.getOaid()).setAndroidId(a.getAndroidId());
                                    feedbackLogs.add(feedbackLog.setCreateTime(new Date()).setMid(a.getMid()).setEventType(1).setActiveChannel(a.getChannel()).setActiveTime(a.getActiveTime())
                                            .setMatchField(key).setCoid(a.getCoid()).setNcoid(a.getNcoid()).setPlot(a.getPlot()).setTs(c.getTs()));
                                    String value = ReflectionUtils.getFieldValue(a, key).toString();
                                    if (key.equals("ipua")) {
                                        ipuaNewUsers.add(new IpuaNewUser().setCoid(a.getCoid()).setNcoid(a.getNcoid()).setIp(a.getIp()).setUa(a.getUa()).setIpua(value).setCreateTime(new Date()));
                                        if (StringUtils.isNotBlank(a.getImei()) && !filters.contains(a.getImei()))
                                            addDayCache(key, Collections.singletonList(new DayHistory().setCoid(a.getCoid()).setNcoid(a.getNcoid()).setWd("imei").setValue(a.getImei())));
                                    }
                                    DayHistory history = new DayHistory().setWd(key).setValue(value).setCoid(a.getCoid()).setNcoid(a.getNcoid()).setCreateTime(new Date());
                                    if (etprop.getMultipleImei() && key.equals("imei")) {
                                        String iimei = a.getIimei();
                                        if (StringUtils.isNotBlank(iimei)) {
                                            List<DayHistory> imeiList = new ArrayList<>();
                                            String[] imeis = iimei.split(",");
                                            DayHistory imeiHistory = new DayHistory();
                                            for (String v : imeis) {
                                                BeanUtils.copyProperties(history, imeiHistory);
                                                if (StringUtils.isNotBlank(v) && !v.equals(value))
                                                    imeiList.add(imeiHistory.setValue(v));
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
                                    if (StringUtils.isNotBlank(c.getCallbackUrl())) {
                                        List<String> errors = errorCache.getIfPresent("errors");
                                        if (errors == null)
                                            errorCache.put("errors", Lists.newArrayList(c.getCallbackUrl()));
                                        else errors.add(c.getCallbackUrl());
                                    }
                                }
                            } catch (Exception e1) {
                                logger.error(e1.getMessage());
                            }
                        });
                    }

                    oldUsers.stream().collect(Collectors.groupingBy(o -> o.getCoid() + "," + o.getNcoid())).forEach((s, activeLoggers) -> {
                        Set<String> collect = activeLoggers.stream().map(o -> ReflectionUtils.getFieldValue(o, (key.equals("ipua") ? key : key + "Md5")).toString()).collect(Collectors.toSet());
                        Page.create(1, 500, i -> new ArrayList<>(collect)).forEach(strings -> {
                            Example example = new Example(ActiveLogger.class);
                            example.createCriteria().andIn((key.equals("ipua") ? key : key + "Md5"), strings)
//                                .andIn("id",ids)
                                    .andEqualTo("coid", Integer.valueOf(s.split(",")[0])).andEqualTo("ncoid", Integer.valueOf(s.split(",")[1]));
                            activeLoggerMapper.updateByExampleSelective(new ActiveLogger().setStatus(1), example);
                        });
                    });
                });
                logger.info("{}end {},{},{}",sd, key, watch.toString());
            });

            Page.create(feedbackLogs).forEach(o -> executor.execute(() -> feedbackLogMapper.insertList(o)));
            Page.create(ipuaNewUsers).forEach(o -> executor.execute(() -> ipuaNewUserMapper.insertList(o)));
            Page.create(histories).forEach(o -> executor.execute(() -> dayHistoryMapper.insertList(o)));
            return success.get();
        }, FEED_BACK, sc);
    }

    private boolean countHistory(DayHistory s) {
        String key = s.getWd();
        try {
            if (redis != null) {
                Double process = redis.process(j -> j.zscore(getDayCacheRedisKey(key), s.getCoid() + "##" + s.getNcoid() + "##" + s.getValue()));
//                boolean exist = process != null && process > 0;
//                if (!exist && redis.process(j -> j.zcount(getDayCacheRedisKey(key), (double) 0, (double) Long.MAX_VALUE) <= 0)) {
//                    getDayCache(key);
//                    process = redis.process(j -> j.zscore(getDayCacheRedisKey(key), s.getCoid() + "##" + s.getNcoid() + "##" + s.getValue()));
//                    return process != null && process > 0;
//                }
                return process != null && process > 0;
            } else {
                List<DayHistory> histories = dayCache.getIfPresent(key);
                return histories == null || histories.stream().anyMatch(o -> o.equals(s));
            }
        } catch (Exception e) {
            logger.error("count_error:{}", e.getMessage());
            Example example = new Example(DayHistory.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("wd", key).andEqualTo("value", s.getValue()).andEqualTo("coid", s.getCoid());
            criteria.andEqualTo("ncoid", s.getNcoid());
            return dayHistoryMapper.selectCountByExample(example) > 0;
        }
    }

    private Boolean check(String key, ActiveLogger activeLogger) {
        return queryMap.keySet().stream().filter(s -> !s.equals(key)).anyMatch(s -> {
            if (StringUtils.isNotBlank(activeLogger.getIimei())) {
                boolean anyMatch = Arrays.stream(activeLogger.getIimei().split(",")).anyMatch(s1 -> !filters.contains(s1) && countHistory(new DayHistory().setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd("imei").setValue(s1)));
                if (anyMatch) return true;
            }
            Object object = ReflectionUtils.getFieldValue(activeLogger, s);
            String fieldValue = object == null ? null : object.toString();
            return !filters.contains(fieldValue) && countHistory(new DayHistory().setValue(fieldValue).setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd(s));
        });
    }



    @Override
    public void syncActive(ShardingContext c) {
        int item = c.getShardingItem();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        List<String> channel = Lists.newArrayList(etprop.getChannel());
        String types = etprop.getTypes();
        if (StringUtils.isNotBlank(types)) {
            channel = Arrays.asList(types.split(","));
        }
        List<String> finalChannel = etprop.getAllAttributed() ? null : channel;
        tryWork(r -> {
            Date date = new Date();
            Long current = date.getTime();
            Long offset = TimeUnit.SECONDS.toMillis(20);
            List<ActiveLogger> data;
            Date maxActiveTime;
            // 跨天AB表处理
            String sd = sds.get(item);
            String sdk = "active" + sd;

            Double process = null;//toutiao-feedback-dir_dev#dayHistory#sync_active
            if (redis != null) process = redis.process(j -> j.zscore(getDayCacheRedisKey("sync_active"), item + "##" + item + "##" + sd));
            if (process != null) {
                maxActiveTime = new Date(process.longValue());
            } else
                maxActiveTime = Optional.ofNullable(dsl.select(activeLogger.activeTime.max()).from(activeLogger).where(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).goe(sd.split(",")[0]).and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).loe(sd.split(",")[1]))
//                        .between(,sd.split(",")[1])
                ).fetchOne()).orElse(new Date(current - TimeUnit.MINUTES.toMillis(etprop.syncOffset)));
            log.info("{}maxtime: {}, sd: {}",item,maxActiveTime,sd);

            int count = etprop.getPreFetchActive();
            if (etprop.getAllAttributed()) {
                if (!format.format(date).equals(format.format(new Date(current + offset)))) {
                    data = webLogMapper.getThirdActiveLogger("ActiveLogger",count ,maxActiveTime, etprop.getSc(), sd.split(",")[0],sd.split(",")[1]);
                    data.addAll(webLogMapper.getThirdActiveLogger("ActiveLogger_B", count, maxActiveTime, etprop.getSc(), sd.split(",")[0], sd.split(",")[1]));
                } else
                    data = webLogMapper.getThirdActiveLogger(webLogMapper.getTableName(), count, maxActiveTime, etprop.getSc(), sd.split(",")[0], sd.split(",")[1]);
            } else {
                if (!format.format(date).equals(format.format(new Date(current + offset)))) {
                    data = feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger", maxActiveTime, etprop.getSc(), sd.split(",")[1]);
                    data.addAll(feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger_B", maxActiveTime, etprop.getSc(), sd.split(",")[1]));
                } else
                    data = feedBackMapper.getThirdActiveLogger(finalChannel, feedBackMapper.getTableName(), maxActiveTime, etprop.getSc(), sd.split(",")[1]);
            }

            List<ActiveLogger> active = activeLoggerCache.getIfPresent(sdk);
            List<ActiveLogger> iimeiActive = new ArrayList<>();

            data = data.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getIimei() + o.getCoid() + o.getNcoid()))), ArrayList::new));
            data = data.parallelStream().filter(o -> (active == null || !active.contains(o))
//                    &&!countHistory(new DayHistory().setWd("imei").setValue(o.getImei()).setCoid(o.getCoid()).setNcoid(o.getNcoid()))
            ).collect(Collectors.toList());

            data.forEach(activeLogger -> {
                if (activeHandler != null) activeHandler.handler(activeLogger);
                activeLogger.setImeiMd5(getMd5StrWithPlaceholder(activeLogger.getImei()))
                        .setAndroidIdMd5(getMd5StrWithPlaceholder(activeLogger.getAndroidId()))
                        .setOaidMd5(getMd5StrWithPlaceholder(activeLogger.getOaid()))
                        .setWifimacMd5(getMd5StrWithPlaceholder(activeLogger.getWifimac()))
                        .setIpua(getMd5StrWithPlaceholder(activeLogger.getIp() + "#" + activeLogger.getUa()))
                        .setCreateTime(new Date()).setPlot(1).setSd(item).setStatus(0);
                String iimei = activeLogger.getIimei();
                if (etprop.getMultipleImei() && StringUtils.isNotBlank(iimei)) {
                    String[] imeis = iimei.split(",");
                    for (int i = 0; i < imeis.length; i++) {
                        String placeholder = "imei#";
                        String currentImei = imeis[i];
                        if (StringUtils.isNotBlank(currentImei) && !currentImei.equals(activeLogger.getImei())) {
                            ActiveLogger e = new ActiveLogger();
                            BeanUtils.copyProperties(activeLogger, e);
                            iimeiActive.add(e.setIpua(placeholder).setOaidMd5(placeholder).setAndroidIdMd5(placeholder).setImei(currentImei).setImeiMd5(getMd5StrWithPlaceholder(currentImei)).setPlot(2));
                        }
                    }
                }
            });
            if (!CollectionUtils.isEmpty(iimeiActive)) data.addAll(iimeiActive);
            if (!CollectionUtils.isEmpty(data)) {
                Date activeTime = data.stream().max(Comparator.comparing(ActiveLogger::getActiveTime)).get().getActiveTime();
                Page.create(data).forEachParallel(activeLoggers -> activeLoggerMapper.insertList(activeLoggers));
                activeLoggerCache.invalidate(sdk);
                if (!CollectionUtils.isEmpty(iimeiActive)) data.removeAll(iimeiActive);
                activeLoggerCache.put(sdk, data);
                addDayCache("sync_active", Collections.singletonList(new DayHistory().setCoid(item).setNcoid(item).setValue(sd).setCreateTime(activeTime)));
                log.info("{}maxtimenex: {} sd: {}",item,activeTime,sd);
            }
            return data.size();
        }, SYNC_ACTIVE, c);

    }

    @Override
//    @Transactional
    public void clean(JobType type, ShardingContext c) {
        Integer offset = etprop.getOffset();
        switch (type) {
            case CLEAN_IMEI:
                Long current = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1);
                tryWork(r -> {
                            Example example = new Example(DayHistory.class);
                            Example.Criteria criteria = example.createCriteria();
                            criteria.andLessThan("createTime", new Date(current));
                            return clickLogMapper.deleteByExample(example);
                        },
                        CLEAN_IMEI, c);
                queryMap.keySet().forEach(s -> {
//                    Long count = dsl.selectDistinct(dayHistory.coid, dayHistory.ncoid, dayHistory.value).from(dayHistory).where(dayHistory.wd.eq(s).and(dayHistory.createTime.gt(new Date(offset)))).fetchCount();
//                    Long process = redis.process(j -> j.zcount(s, offset, Long.MAX_VALUE));
//                    if (process - count > 10) {
//                        logger.error("cache_verify_error:process:{},count:{}", process, count);
//                        clearCache(null);
//                    } else clearCache(current);
                });
                break;
            case CLEAN_ACTIVE:
                tryWork(r -> {
                    if (!etprop.getPersistActive()) {
                        Example example = new Example(ActiveLogger.class);
                        Example.Criteria criteria = example.createCriteria();
                        criteria.andLessThan("createTime", new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(etprop.getCleanActiveOffset())));
                        activeLoggerMapper.deleteByExample(example);
                    }
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
                            .where(activeLogger.createTime.before(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(etprop.getCleanActiveOffset())))).limit(10000).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;
                    cleanActiveLogger(activeLoggers,etprop.getPersistActive());
                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE, c);
                break;
            case CLEAN_ACTIVE_HISTORY:
                tryWork(r -> {
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
                            .where(activeLogger.status.gt(0)
//                                    .and(activeLogger.createTime.before(JPAExpressions.select(activeLogger.createTime.max()).from(activeLogger)))
                            ).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;

                    cleanActiveLogger(activeLoggers, etprop.getPersistActive());
                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE_HISTORY, c);
                break;
            case CLEAN_CLICK:
                tryWork(r -> {
                            List<ClickLog> clickLogs = dsl.selectFrom(clickLog).setLockMode(LockModeType.NONE)
                                    .where(clickLog.createTime.before(new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(Optional.ofNullable(etprop.getClickDataExpire()).orElse(offset))))).fetch();
                            if (CollectionUtils.isEmpty(clickLogs)) return 0;

                            List<ClickLogHistory> list = clickLogs.stream().map(clickLog1 -> {
                                ClickLogHistory clickLogHistory = new ClickLogHistory();
                                BeanUtils.copyProperties(clickLog1, clickLogHistory);
                                clickLogHistory.setId(null);
                                return clickLogHistory;
                            }).collect(Collectors.toList());

                            if (etprop.getPersistClick()) {
                                Page.create(list).forEach(data -> clickLogHistoryMapper.insertList(data));
                            }
                            Page.create(1000, clickLogs.stream().map(ClickLog::getId).collect(Collectors.toList())).forEach(longs -> {
                                Example example = new Example(ClickLog.class);
                                Example.Criteria criteria = example.createCriteria();
                                criteria.andIn("id", longs);
                                clickLogMapper.deleteByExample(example);
                            });
                            return (long) clickLogs.size();
                        },
                        CLEAN_CLICK, c);
                break;
        }

    }

    @Override
    public void clearCache(Long offset) {
        dayCache.invalidateAll();
        if (redis != null) {
            if (offset == null) {
                queryMap.keySet().forEach(s -> {
                    Long zexpire = redis.del(getDayCacheRedisKey(s));
                    logger.info("clear_cache:{}", zexpire);
                });
            } else {
                queryMap.keySet().forEach(s -> redis.zexpire(getDayCacheRedisKey(s), (double) 0, (double) offset));
            }
        }
    }

    @Override
    public void syncCache() {
        clearCache(null);
        queryMap.keySet().forEach(this::getDayCache);
    }

    private void cleanActiveLogger(List<ActiveLogger> activeLoggers, Boolean persistActive) {
        List<ActiveLoggerHistory> histories = activeLoggers.stream().map(a -> {
            ActiveLoggerHistory activeLoggerHistory = new ActiveLoggerHistory();
            BeanUtils.copyProperties(a, activeLoggerHistory);
            activeLoggerHistory.setId(null);
            return activeLoggerHistory;
        }).collect(Collectors.toList());
        if (persistActive)
            Page.create(histories).forEachParallel(list -> activeLoggerHistoryMapper.insertList(list));
        List<Long> ids = activeLoggers.stream().map(ActiveLogger::getId).collect(Collectors.toList());
        Page.create(1000, ids).forEach(longs -> {
            Example example = new Example(ActiveLogger.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andIn("id", longs);
            activeLoggerMapper.deleteByExample(example);
        });
    }

    @Override
    public void stat(JobType type, ShardingContext c) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        tryWork(r -> feedBackMapper.activeStaticesDay(format.format(new Date()), ",did"), STAT_DAY, c);
    }

    @Override
    public void secondStay(JobType type, ShardingContext c) {
        tryWork(jobType -> {
            List<ThirdRetentionLog> list = feedBackMapper.getSecondStay(etprop.getChannel());
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
//                        feedBackMapper.insertDayLiucunImei(thirdRetentionLog.getImei(), thirdRetentionLog.getImeimd5());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    log.error(e.getMessage());
                }
            }
            return success;
        }, RETENTION, c);
    }

    @Autowired
    DistributedLock lock;
    private void tryWork(Function<JobType, Object> consumer, JobType type, ShardingContext c) {
        String k = String.format("%s#%s", etprop.getChannel(), type.getKey());
        Integer mode = etprop.getMode();
//        String sync = getDayCacheRedisKey("sync##");
        try {
            if (!Lists.newArrayList(SYNC_ACTIVE,FEED_BACK).contains(type)) {
                if (c.getShardingItem() != 0) {
                    logger.info("skip task {} ", k);
                    return;
                }
            }
//            if (type == FEED_BACK) {
//                redis.process(j -> j.setnx(sync, ""));
//            }
//            else if (type == SYNC_ACTIVE) {
//                Boolean process = redis.process(j -> j.exists(sync));
//                if (process) return;
//            }
            logger.info("start {}{} {}", k, c.getShardingItem());
            Long start = System.currentTimeMillis();
            if (mode == 2 && redis.process(jedis -> jedis.setnx(k, "")).equals(0L)) {
                throw new RuntimeException(type.getKey() + " failed because redis setnx return 0");
            }
//            AtomicReference<Object> r = new AtomicReference<>();
//            if (type==FEED_BACK) {
//                lock.lock(FEED_BACK.name(),FEED_BACK.name(),() -> {
//                    r.set(consumer.apply(type));
//                });
//            }
//            else r.set(consumer.apply(type));

            Object r = consumer.apply(type);
            if (mode == 2) redis.expire(k, type.getExpire().intValue());
            long end = System.currentTimeMillis() - start;
            logger.info("finished {}{} in {}ms,count:{}", k, c.getShardingItem(), end, r.toString());
            if (end > 300000 && etprop.getWarning())
                fsService.sendMsg(String.format("%s-%s finished in %d at %s , {}", appName, k, end, new Date()), r.toString());
        } catch (Exception e) {
            e.printStackTrace();
            fsService.sendMsg(String.format("%s-%s error -> %s", appName, type.getKey(), e.getMessage()));
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
                Map<String, Double> map = new HashMap<>(dayHistories.size());
                dayHistories.forEach(dayHistory -> map.put(String.format("%d##%d##%s", dayHistory.getCoid(), dayHistory.getNcoid(), dayHistory.getValue()), (double) dayHistory.getCreateTime().getTime()));
                redis.process(j -> j.zadd(redisKey, map));
            } else {
                List<DayHistory> list = dayCache.getIfPresent(key);
                if (CollectionUtils.isEmpty(list)) {
                    dayCache.put(key, dayHistories);
                } else list.addAll(dayHistories);
            }
        } catch (Exception e) {
//            redis = null;
            logger.error("add_cache_error" + e.getMessage());
        }
    }

    @Override
    public List<DayHistory> getDayCache(String key) {
        List<DayHistory> dayHistories = null;
        Integer offset = etprop.getOffset();
        try {
            Boolean flag = false;
            if (redis != null) {
                String redisKey = getDayCacheRedisKey(key);
                long end = System.currentTimeMillis();
//                dayHistories = redis.zrange(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) end + TimeUnit.HOURS.toMillis(8)).stream().map(s -> {
//                    String[] split = s.split("##");
//                    String value = split.length < 3 ? "" : split[2];
//                    return new DayHistory().setWd(key).setCoid(split[0] == null ? null : Integer.valueOf(split[0]))
//                            .setNcoid(split[1] == null ? null : Integer.valueOf(split[1])).setValue(value);
//                }).collect(Collectors.toList());
                Long process = redis.process(j -> j.zcount(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) Long.MAX_VALUE));
//                logger.info("get_redis:{}", dayHistories == null ? "0" : dayHistories.size());
                if (process <= 0) flag = true;
            } else {
                dayHistories = dayCache.getIfPresent(key);
                flag = CollectionUtils.isEmpty(dayHistories);
            }
            if (CollectionUtils.isEmpty(dayHistories)) {
                Example example = new Example(DayHistory.class);
                Example.Criteria criteria = example.createCriteria();
                criteria.andEqualTo("wd", key);
                criteria.andGreaterThan("createTime", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1)));
                dayHistories = dayHistoryMapper.selectByExample(example);
                if (flag) addDayCache(key, dayHistories);
            }
        } catch (Exception e) {
            logger.error("redis_cache_error: {}", e.getMessage());
//            Example example = new Example(DayHistory.class);
//            Example.Criteria criteria = example.createCriteria();
//            criteria.andEqualTo("wd", key);
//            criteria.andGreaterThan("createTime", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1)));
//            dayHistories = dayHistoryMapper.selectByExample(example);
//            addDayCache(key, dayHistories);
            e.printStackTrace();
        }
        return dayHistories;
    }

    @Override
    public void afterPropertiesSet() {
        if (StringUtils.isNotBlank(etprop.getFilters())) filters.addAll(Arrays.asList(etprop.getFilters().split(",")));
        BooleanExpression imeiBe = activeLogger.imeiMd5.eq(clickLog.imeiMd5);
        BooleanExpression oaidBe = activeLogger.oaidMd5.eq(clickLog.oaidMd5);
        BooleanExpression androidIdBe = activeLogger.androidIdMd5.eq(clickLog.androidIdMd5);
        if (etprop.getMacAttributed()) androidIdBe = androidIdBe.and(activeLogger.wifimacMd5.eq(clickLog.mac));
        BooleanExpression ipuaBe = activeLogger.ipua.eq(clickLog.ipua);
        Map<String, BooleanExpression> wd = new LinkedHashMap<>();

        wd.put("imei", imeiBe);
        wd.put("oaid", oaidBe.and(imeiBe.not()));
        wd.put("androidId", androidIdBe.and(imeiBe.not()).and(oaidBe.not()));
        if (etprop.getIpAttributed()) {
            BooleanExpression booleanExpression = ipuaBe.and(imeiBe.not()).and(oaidBe.not()).and(androidIdBe.not());
            if (!CollectionUtils.isEmpty(ipuaChannels) && !(ipuaChannels.size() == 1 && StringUtils.isBlank(ipuaChannels.get(0)))) {
                booleanExpression = booleanExpression.and(activeLogger.channel.in(ipuaChannels));
            }
            wd.put("ipua", booleanExpression);
        }
//activeLogger.id,activeLogger.imei,activeLogger.oaid,activeLogger.androidId,activeLogger.oaid,
//                    activeLogger.ncoid,activeLogger.ipua,activeLogger.wifimacMd5,activeLogger.wifimac,activeLogger.channel
//                    , clickLog.clickTime,clickLog.callbackUrl,clickLog.channel,clickLog.param1,clickLog.param2,clickLog.param3,clickLog.param4
        wd.forEach((s, e) -> {
            if (!s.equals("imei")) e = e.and(activeLogger.plot.eq(2));
            if (!CollectionUtils.isEmpty(range) && !(range.size() == 1 && StringUtils.isBlank(range.get(0))))
                e = e.and(activeLogger.channel.in(range));
            if (!CollectionUtils.isEmpty(filterChannels) && !(filterChannels.size() == 1 && StringUtils.isBlank(filterChannels.get(0))))
                e = e.and(activeLogger.channel.notIn(filterChannels));
            if (etprop.getDatetimeAttributed())
                e = e.and(activeLogger.activeTime.after(clickLog.clickTime));
            if (etprop.getMatchMinuteOffset() > 0)
                e = e.and(Expressions.
                        booleanTemplate("abs(datediff(minute,{0},{1})) > {2}", clickLog.clickTime, activeLogger.activeTime, etprop.getMatchMinuteOffset()));
            //dsl.select(activeLogger, clickLog).from(activeLogger).setLockMode(LockModeType.NONE).innerJoin(clickLog).on(e)
            queryMap.put(s, e);
        });
        if (etprop.clearCache) {
            clearCache(null);
            wd.keySet().forEach(s -> getDayCache(s));
        }
        redis.del(getDayCacheRedisKey("sync_active"));
        int sc = etprop.getSc();
        sds = sharding(sc);
    }

    public static void main(String[] args) {
        List<Integer> taskList = new ArrayList<>();
        for (int i = 0; i <60; i++) {
            taskList.add(i);
        }
        int total = taskList.size();
        int threadNum = 5;
        int remaider = total % threadNum; // 计算出余数
        int number = total / threadNum; // 计算出商
        int offset = 0;// 偏移量
        for (int i = 0; i < threadNum; i++) {
            if (remaider > 0) {
                List<Integer> subList = taskList.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
                System.out.println(subList.get(0) + "--" + subList.get(subList.size() - 1));
            } else {
                List<Integer> subList = taskList.subList(i * number + offset, (i + 1) * number + offset);
                System.out.println(subList.get(0) + "--" + subList.get(subList.size() - 1));
            }
        }
    }

    private List<String> sharding(Integer sc) {
        List<Integer> taskList = new ArrayList<>();
        List<String> ret = new ArrayList<>();

        for (int i = 0; i <60; i++) {
            taskList.add(i);
        }
        int total = taskList.size();
        int threadNum = sc;
        int remaider = total % threadNum; // 计算出余数
        int number = total / threadNum; // 计算出商
        int offset = 0;// 偏移量
        for (int i = 0; i < threadNum; i++) {
            if (remaider > 0) {
                List<Integer> subList = taskList.subList(i * number + offset, (i + 1) * number + offset + 1);
                remaider--;
                offset++;
                ret.add(subList.get(0) + "," + subList.get(subList.size() - 1));
            } else {
                List<Integer> subList = taskList.subList(i * number + offset, (i + 1) * number + offset);
                ret.add(subList.get(0) + "," + subList.get(subList.size() - 1));
            }
        }
        return ret;
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
