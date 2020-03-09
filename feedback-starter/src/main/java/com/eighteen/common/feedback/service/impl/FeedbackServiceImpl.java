package com.eighteen.common.feedback.service.impl;


import com.alibaba.fastjson.JSONObject;
import com.dangdang.ddframe.job.api.ShardingContext;
import com.eighteen.common.distribution.DistributedLock;
import com.eighteen.common.distribution.ZooKeeperConnector;
import com.eighteen.common.feedback.EighteenProperties;
import com.eighteen.common.feedback.dao.*;
import com.eighteen.common.feedback.domain.ThirdRetentionLog;
import com.eighteen.common.feedback.entity.*;
import com.eighteen.common.feedback.entity.dao2.ActiveLoggerDao;
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
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;
import redis.clients.jedis.ZParams;
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
    @Autowired
    ZooKeeperConnector zooKeeperConnector;
    @Autowired
    DistributedLock lock;
    @Autowired
    ActiveLoggerDao activeLoggerDao;
    @Autowired(required = false)
    private Redis redis;
    @Value("${log.env:dev}")
    String env;
    @Autowired
    private RedisTemplate redisTemplate;
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
    private Cache<String, Set<ActiveLogger>> activeLoggerCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1)
            .build();
    private Cache<String, List<String>> errorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES).concurrencyLevel(1)
            .build();
    private Map<String, BooleanExpression> queryMap = new LinkedHashMap<>(4);


    public FeedbackServiceImpl(EighteenProperties properties) {
        this.etprop = properties;
    }

    public static void main(String[] args) {
        List<Integer> taskList = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
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

    public static String[] prodKeys() {
        return null;
    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void feedback(ShardingContext sc) {
        tryWork(r -> {
            AtomicLong success = new AtomicLong(0);
            List<DayHistory> histories = Collections.synchronizedList(new ArrayList<>());
            List<FeedbackLog> feedbackLogs = Collections.synchronizedList(new ArrayList<>());
            List<IpuaNewUser> ipuaNewUsers = Collections.synchronizedList(new ArrayList<>());
            StopWatch query = StopWatch.createStarted();
            String[] sd = sds.get(sc.getShardingItem()).split(",");
            Date date = Optional.ofNullable(dsl.select(activeLogger.activeTime.max()).from(activeLogger).fetchOne()).orElse(new Date());
            logger.info("{} 开始查询 ", sd, query.toString());
            Map<String, List<ActiveLogger>> map = queryMap.entrySet().parallelStream().collect(Collectors.toMap(Map.Entry::getKey, e ->
                    dsl.select(activeLogger, clickLog).from(activeLogger).setLockMode(LockModeType.NONE).innerJoin(clickLog).on(e.getValue())
                            .where(activeLogger.status.eq(0)
//                                .and(activeLogger.sd.eq(sc == null ? 0 : sc.getShardingItem()))
                                            .and(activeLogger.activeTime.goe(new Date(date.getTime() - TimeUnit.MINUTES.toMillis(etprop.getActiveMinuteOffset())))
                                            )
//                                            .and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).goe(sd[0]).and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).loe(sd[1])))
                            ).limit(Long.valueOf(etprop.getPreFetch())).fetch().stream().map(tuple -> tuple.get(activeLogger).setClickLog(tuple.get(clickLog))).collect(Collectors.toList())));
            logger.info("{} 查询耗时:,{}", sd, query.toString());
            map.forEach((key, e) -> {
                try {
                    if (CollectionUtils.isEmpty(e)) return;
                    List<ActiveLogger> list = e.stream()
                            .sorted((o1, o2) -> o2.getClickLog().getClickTime().compareTo(o1.getClickLog().getClickTime()))
                            .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> gekeyString(key, o2)))), ArrayList::new)
                            );
                    if (etprop.getDoindb())
                        lock.lock(getDayCacheRedisKey(FEED_BACK.name()), appName + FEED_BACK.name(), () -> doIndb(success, histories, feedbackLogs, ipuaNewUsers, key, list));
                    else doIndb(success, histories, feedbackLogs, ipuaNewUsers, key, list);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    logger.error("step 去重 ,{}", e1.getMessage());
                    fsService.sendMsg(e1.getMessage());
                }
            });

            if(env.equals("pro")) handerResult(histories, feedbackLogs, ipuaNewUsers);
            return success.get();
        }, FEED_BACK, sc);
    }

    private void doIndb(AtomicLong success, List<DayHistory> histories, List<FeedbackLog> feedbackLogs, List<IpuaNewUser> ipuaNewUsers, String key, List<ActiveLogger> list) {
        List<ActiveLogger> oldUsers = new ArrayList<>();
        handlerFeedback(success, histories, feedbackLogs, ipuaNewUsers, key, oldUsers, list);

        try {
            if (oldUsers.size() > 0)
                oldUsers.stream().collect(Collectors.groupingBy(o -> o.getCoid() + "," + o.getNcoid())).forEach((s, activeLoggers) -> {
                    Set<String> collect = activeLoggers.stream().map(o -> {
                        Object value = ReflectionUtils.getFieldValue(o, (key.equals("ipua") ? key : key + "Md5"));
                        if (value.equals("")) {
                            logger.error("step update error:key{}, {}", key, o.toString());
                            return "??";
                        }
                        return value.toString();
                    }).collect(Collectors.toSet());

                   if (env.equals("pro"))Page.create(500, new ArrayList<>(collect)).forEach(strings -> {
                        Example example = new Example(ActiveLogger.class);
                        example.createCriteria().andIn((key.equals("ipua") ? key : key + "Md5"), strings)
                                .andEqualTo("coid", Integer.valueOf(s.split(",")[0])).andEqualTo("ncoid", Integer.valueOf(s.split(",")[1]));
                        activeLoggerMapper.updateByExampleSelective(new ActiveLogger().setStatus(1), example);
                    });
                });
        } catch (Exception e) {
            logger.error("step update stauts error :{}" + e.getMessage());
        }
    }

    private void handlerFeedback(AtomicLong success, List<DayHistory> histories, List<FeedbackLog> feedbackLogs, List<IpuaNewUser> ipuaNewUsers, String key, List<ActiveLogger> oldUsers, List<ActiveLogger> filter) {
//        List<DayHistory> dayHistoryList = getDayCache(key);
        StopWatch query = StopWatch.createStarted();
        logger.info("开始去重:,{}", query.toString());
        filter.parallelStream().forEach(activeLogger -> {
            Object fieldValue = ReflectionUtils.getFieldValue(activeLogger, key);
            if (fieldValue == null) return;
            String value = fieldValue.toString();
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
        logger.info("去重耗时:,{}", query.toString());

//        List<String> retErrors = errorCache.getIfPresent("errors");
//        filter = filter.stream()
//                .filter(o -> !oldUsers.contains(o) && (retErrors == null || !retErrors.contains(o.getClickLog().getCallbackUrl())))
//                .collect(Collectors.toList());

        logger.info("老用户去重:,{}", query.toString());
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
            logger.info("老用户去重耗时:,{}", query.toString());

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
//                        oldUsers.add(a);
                        Optional.ofNullable(oldUsers).ifPresent(activeLoggers -> activeLoggers.add(a));
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
            logger.info("回传耗时:,{}", query.toString());
        }
    }

    private boolean countHistory(DayHistory s) {
        String key = s.getWd();
        try {
            if (redis != null) {
                Double score = redisTemplate.opsForZSet().score(getDayCacheRedisKey(key), String.format("%d##%d##%s", s.getCoid(), s.getNcoid(), s.getValue()));
                return score != null && score > 0;
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

//    private DayHistory checkHistory(DayHistory s) {
//        String key = s.getWd();
//        try {
//            if (redis != null) {
//                Double process = redis.process(j -> j.zscore(getDayCacheRedisKey(key), String.format("%d##%d##%s", s.getCoid(), s.getNcoid(), s.getValue())));
//
//                if (process != null && process > 0) s.setStatus(1);
//            } else {
//                List<DayHistory> histories = dayCache.getIfPresent(key);
//                if (histories == null || histories.stream().anyMatch(o -> o.equals(s))) {
//                    s.setStatus(1);
//                }
//            }
//        } catch (Exception e) {
//            logger.error("count_error:{}", e.getMessage());
//            Example example = new Example(DayHistory.class);
//            Example.Criteria criteria = example.createCriteria();
//            criteria.andEqualTo("wd", key).andEqualTo("value", s.getValue()).andEqualTo("coid", s.getCoid());
//            criteria.andEqualTo("ncoid", s.getNcoid());
//            if (dayHistoryMapper.selectCountByExample(example) > 0) s.setStatus(1);
//        }
//        return s;
//    }

    private Boolean check(String key, ActiveLogger activeLogger) {
        return queryMap.keySet().stream().filter(s -> {
            Object fieldValue = ReflectionUtils.getFieldValue(activeLogger, s);
            return !s.equals(key)&& fieldValue !=null&&StringUtils.isNotBlank(fieldValue.toString());
        }).anyMatch(s -> {
            if (StringUtils.isNotBlank(activeLogger.getIimei())) {
                boolean anyMatch = Arrays.stream(activeLogger.getIimei().split(",")).anyMatch(s1 -> !filters.contains(s1)
                                && countHistory(new DayHistory().setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd("imei").setValue(s1))
//                        && feedBackMapper.count("imei",s1,activeLogger.getCoid(),activeLogger.getNcoid())>0
                );
                if (anyMatch) return true;
            }
            Object object = ReflectionUtils.getFieldValue(activeLogger, s);
            String fieldValue = object == null ? null : object.toString();
            return !filters.contains(fieldValue) &&
                    countHistory(new DayHistory().setValue(fieldValue).setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd(s));
//                    feedBackMapper.count(s,fieldValue,activeLogger.getCoid(),activeLogger.getNcoid())>0;
        });
    }

    private Boolean check(String key, ActiveLogger activeLogger, List<DayHistory> dayHistories) {
        return queryMap.keySet().stream().filter(s -> !s.equals(key)).anyMatch(s -> {
            if (StringUtils.isNotBlank(activeLogger.getIimei())) {
                boolean anyMatch = Arrays.stream(activeLogger.getIimei().split(",")).anyMatch(s1 -> !filters.contains(s1)
                        && dayHistories.contains(new DayHistory().setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd("imei").setValue(s1)));
                if (anyMatch) return true;
            }
            Object object = ReflectionUtils.getFieldValue(activeLogger, s);
            String fieldValue = object == null ? null : object.toString();
            return !filters.contains(fieldValue) && dayHistories.contains(new DayHistory().setValue(fieldValue).setCoid(activeLogger.getCoid()).setNcoid(activeLogger.getNcoid()).setWd(s));
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
            if (redis != null)
                process = redis.process(j -> j.zscore(getDayCacheRedisKey("sync_active"), item + "##" + item + "##" + sd));
            if (process != null) {
                maxActiveTime = new Date(process.longValue());
            } else
                maxActiveTime = Optional.ofNullable(dsl.select(activeLogger.activeTime.max()).from(activeLogger).where(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).goe(sd.split(",")[0]).and(Expressions.stringTemplate("DATEPART(ss,{0})", activeLogger.activeTime).loe(sd.split(",")[1]))
//                        .between(,sd.split(",")[1])
                ).fetchOne()).orElse(new Date(current - TimeUnit.MINUTES.toMillis(etprop.syncOffset)));
            log.info("{} 开始激活时间 : {}, sd: {}", item, maxActiveTime, sd);

            int count = etprop.getPreFetchActive();
            if (etprop.getAllAttributed()) {
                if (!format.format(date).equals(format.format(new Date(current + offset)))) {
                    data = webLogMapper.getThirdActiveLogger("ActiveLogger", count, maxActiveTime, etprop.getSc(), sd.split(",")[0], sd.split(",")[1]);
                    data.addAll(webLogMapper.getThirdActiveLogger("ActiveLogger_B", count, maxActiveTime, etprop.getSc(), sd.split(",")[0], sd.split(",")[1]));
                } else
                    data = webLogMapper.getThirdActiveLogger(webLogMapper.getTableName(), count, maxActiveTime, etprop.getSc(), sd.split(",")[0], sd.split(",")[1]);
            } else {
                if (!format.format(date).equals(format.format(new Date(current + offset)))) {
                    data = feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger", maxActiveTime, sd.split(",")[0], sd.split(",")[1]);
                    data.addAll(feedBackMapper.getThirdActiveLogger(finalChannel, "ActiveLogger_B", maxActiveTime, sd.split(",")[0], sd.split(",")[1]));
                } else
                    data = feedBackMapper.getThirdActiveLogger(finalChannel, feedBackMapper.getTableName(), maxActiveTime,  sd.split(",")[0], sd.split(",")[1]);
            }
            Date activeTime = data.stream().max(Comparator.comparing(ActiveLogger::getActiveTime)).get().getActiveTime();

            log.info("{} 查询激活数据 : {}, sd: {}", item, maxActiveTime, sd);

            Set<ActiveLogger> active = activeLoggerCache.getIfPresent(sdk);
            List<ActiveLogger> iimeiActive = new ArrayList<>();

            data = data.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getIimei() + o.getCoid() + o.getNcoid()))), ArrayList::new));
            data = data.parallelStream().filter(o -> {
                Boolean b = (StringUtils.isBlank(o.getImei()));
                        if (b) return true;
                        Double score = redisTemplate.opsForZSet().score(getDayCacheRedisKey(String.format("active#imei#%d#%d", o.getCoid(), o.getNcoid())),
                                String.format("%s%s%s", o.getImei(), o.getOaid(), o.getAndroidId()));
                        return ((active == null || !active.contains(o))
                                //                    &&!countHistory(new DayHistory().setWd("imei").setValue(o.getImei()).setCoid(o.getCoid()).setNcoid(o.getNcoid()))
                                && (score == null || score <= 0));
                    }
            ).collect(Collectors.toList());
            log.info("{} 激活去重 : {}, sd: {}", item, maxActiveTime, sd);

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

            log.info("{} 处理多卡 : {}, sd: {}", item, maxActiveTime, sd);

            if (!CollectionUtils.isEmpty(iimeiActive)){
//                Page.create(iimeiActive).forEachParallel(activeLoggers -> activeLoggerMapper.insertList(activeLoggers));
                data.addAll(iimeiActive);
            }
            data = data.stream().collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o -> o.getIimei() + o.getCoid() + o.getNcoid()))), ArrayList::new));
            if (!CollectionUtils.isEmpty(data)) {
                if (etprop.getPersistRedis()) {
                    data.parallelStream().forEach(a -> {
                        ActiveLogger log = activeLoggerDao.save(a);
                        if (StringUtils.isNotBlank(a.getImei()))
                            redisTemplate.opsForZSet().add(getDayCacheRedisKey(String.format("active#imei#%d#%d", a.getCoid(), a.getNcoid())), String.format("%s%s%s", log.getImei(), log.getOaid(), log.getAndroidId()), a.getId().doubleValue());

//                            redis.zadd(getDayCacheRedisKey("active#imei#" + a.getCoid() + "#" + a.getNcoid()), log.getId().doubleValue(), a.getImeiMd5());
//                        if (StringUtils.isNotBlank(a.getOaidMd5()))
//                            redis.zadd(getDayCacheRedisKey("active#oaid#") + a.getCoid() + "#" + a.getNcoid(), log.getId().doubleValue(), a.getOaidMd5());
//                        if (StringUtils.isNotBlank(a.getAndroidIdMd5()))
//                            redis.zadd(getDayCacheRedisKey("active#android#") + a.getCoid() + "#" + a.getNcoid(), log.getId().doubleValue(), a.getAndroidIdMd5());
                    });
                } else {
                    Page.create(data).forEachParallel(activeLoggers -> {
                        activeLoggerMapper.insertList(activeLoggers);
                    });
                }
                log.info("{} 增加激活 : {}, sd: {}", item, maxActiveTime, sd);

                activeLoggerCache.invalidate(sdk);
                if (!CollectionUtils.isEmpty(iimeiActive)) data.removeAll(iimeiActive);
                activeLoggerCache.put(sdk, new HashSet<>(data));
                addDayCache("sync_active", Collections.singletonList(new DayHistory().setCoid(item).setNcoid(item).setValue(sd).setCreateTime(activeTime)));
                log.info("{} 结束激活时间 : {} sd: {}", item, activeTime, sd);
            }


            return data.size()-iimeiActive.size();
        }, SYNC_ACTIVE, c);

    }

    @Override
    @Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public void clean(JobType type, ShardingContext c) {
        Integer offset = etprop.getOffset();
        switch (type) {
            case CLEAN_IMEI:
                Long current = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1);
                tryWork(r -> {
                            Example example = new Example(DayHistory.class);
                            Example.Criteria criteria = example.createCriteria();
                            criteria.andLessThan("createTime", new Date(current));
                            return dayHistoryMapper.deleteByExample(example);
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
                        criteria.andLessThan("activeTime", new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(etprop.getCleanActiveOffset())));
//                        activeLoggerMapper.deleteByExample(example);
                        return  activeLoggerMapper.updateByExampleSelective(new ActiveLogger().setStatus(-1), example);
                    }
                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
                            .where(activeLogger.activeTime.before(new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(etprop.getCleanActiveOffset())))).limit(10000).fetch();
                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;
                    cleanActiveLogger(activeLoggers, etprop.getPersistActive());
                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE, c);
                break;
            case CLEAN_ACTIVE_HISTORY:
                tryWork(r -> {
                    return dsl.delete(activeLogger).where(activeLogger.status.gt(0)).execute();

//                    List<ActiveLogger> activeLoggers = dsl.selectFrom(activeLogger).setLockMode(LockModeType.NONE)
//                            .where(activeLogger.status.gt(0)).fetch();
//                    if (CollectionUtils.isEmpty(activeLoggers)) return 0L;
//
//                    cleanActiveLogger(activeLoggers, etprop.getPersistActive());
//                    return (long) activeLoggers.size();
                }, CLEAN_ACTIVE_HISTORY, c);
                break;
            case CLEAN_CLICK:
                tryWork(r -> {
                            if (!etprop.getPersistClick()) {
                                Example example = new Example(ClickLog.class);
                                Example.Criteria criteria = example.createCriteria();
                                criteria.andLessThan("clickTime", new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(etprop.getClickDataExpire())));
                                return clickLogMapper.deleteByExample(example);
                            }
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

    private void tryWork(Function<JobType, Object> consumer, JobType type, ShardingContext c) {
        String k = String.format("%s#%s", etprop.getChannel(), type.getKey());
        Integer mode = etprop.getMode();
//        String sync = getDayCacheRedisKey("sync##");
        try {
            if (!Lists.newArrayList(SYNC_ACTIVE).contains(type)) {
                if (c.getShardingItem() != 0) {
                    return;
                }
            }
            logger.info("start {}{} {}", k, c.getShardingItem());
            Long start = System.currentTimeMillis();
            if (mode == 2 && redis.process(jedis -> jedis.setnx(k, "")).equals(0L)) {
                throw new RuntimeException(type.getKey() + " failed because redis setnx return 0");
            }

            Object r = consumer.apply(type);
            if (mode == 2) redis.expire(k, type.getExpire().intValue());
            long end = System.currentTimeMillis() - start;
            logger.info("finished {}{} in {}ms,count:{}", k, c.getShardingItem(), end, r.toString());
            if (end > 300000 && etprop.getWarning())
                fsService.sendMsg(String.format("%s-%s finished in %d at %s , {}", appName, k, end, new Date()), r.toString());
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("error {} ,", e.toString());
            logger.error("error {} ,", e.getCause() == null ? "" : e.getCause());
            fsService.sendMsg(String.format("%s-%s error -> %s", appName, type.getKey(), e.getMessage(), e.toString()));
        }
    }

    public String getDayCacheRedisKey(String key) {
        return appName + "#dayHistory#v2#" + key;
    }

    @Override
    public String generMember(Integer coid, Integer ncoid, String value) {
        return coid + "#" + ncoid + "#" + value;
    }

    @Override
    public void addDayCache(String key, List<DayHistory> dayHistories) {
        try {
            if (CollectionUtils.isEmpty(dayHistories)) return;
            if (redis != null) {
                String redisKey = getDayCacheRedisKey(key);
//                Map<String, Double> map = new HashMap<>(dayHistories.size());
//                dayHistories.forEach(dayHistory -> map.put(String.format("%d##%d##%s", dayHistory.getCoid(), dayHistory.getNcoid(), dayHistory.getValue()), (double) dayHistory.getCreateTime().getTime()));
                Long count = redisTemplate.opsForZSet().add(redisKey, dayHistories.stream().map(o -> new DefaultTypedTuple<Object>(String.format("%d##%d##%s", o.getCoid(), o.getNcoid(), o.getValue()), (double) o.getCreateTime().getTime())).collect(Collectors.toSet()));
//                redis.process(j -> j.zadd(redisKey, map));
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
                Set<String> range = redisTemplate.opsForZSet().range(redisKey, end - TimeUnit.DAYS.toMillis(offset + 1), Long.MAX_VALUE);
//                dayHistories = redis.zrange(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) Long.MAX_VALUE).stream().map(s -> {
//                    String[] split = s.split("##");
//                    String value = split.length < 3 ? "" : split[2];
//                    return new DayHistory().setWd(key).setCoid(split[0] == null ? null : Integer.valueOf(split[0]))
//                            .setNcoid(split[1] == null ? null : Integer.valueOf(split[1])).setValue(value);
//                }).collect(Collectors.toList());

                dayHistories = range.stream().map(s -> {
                    String[] split = s.split("##");
                    String value = split.length < 3 ? "" : split[2];
                    return new DayHistory().setWd(key).setCoid(split[0] == null ? null : Integer.valueOf(split[0]))
                            .setNcoid(split[1] == null ? null : Integer.valueOf(split[1])).setValue(value);
                }).collect(Collectors.toList());


//                Long process = redis.process(j -> j.zcount(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) Long.MAX_VALUE));
                Long process = redisTemplate.opsForZSet().count(redisKey, (double) end - TimeUnit.DAYS.toMillis(offset + 1), (double) Long.MAX_VALUE);
//                logger.info("get_redis:{}", dayHistories == null ? "0" : dayHistories.size());
                if (process <= 0) flag = true;
            } else {
                dayHistories = dayCache.getIfPresent(key);
                flag = CollectionUtils.isEmpty(dayHistories);
            }
            if (CollectionUtils.isEmpty(dayHistories)) {
                dayHistories = feedBackMapper.dayHistorys(key, new Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(offset + 1)));
                dayHistories = dayHistories.stream().sorted((o1, o2) -> o2.getCreateTime().compareTo(o1.getCreateTime()))
                        .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> o2.getWd() + o2.getValue() + o2.getCoid() + o2.getNcoid()))), ArrayList::new));
                if (flag && !CollectionUtils.isEmpty(dayHistories)) addDayCache(key, dayHistories);
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
    public Long fb() {
        List<DayHistory> histories = Collections.synchronizedList(new ArrayList<>());
        List<FeedbackLog> feedbacks = Collections.synchronizedList(new ArrayList<>());
        List<IpuaNewUser> ipuas = Collections.synchronizedList(new ArrayList<>());
        AtomicLong count = new AtomicLong();
        queryMap.keySet().forEach(key -> {
            ZParams zParams = new ZParams();
            zParams.aggregate(ZParams.Aggregate.MAX);
            zParams.weightsByDouble(1, 0);
            Jedis jedis = redis.getJedis();
            jedis.zinterstore(getDayCacheRedisKey("zinter"), zParams, getDayCacheRedisKey("click#" + key), getDayCacheRedisKey("active#" + key));

            ZParams params = new ZParams();
            params.weightsByDouble(1, 0);
            params.aggregate(ZParams.Aggregate.MIN);
            String zinter = getDayCacheRedisKey("zinter");
            jedis.zunionstore(zinter, params, "");
            jedis.zremrangeByScore(zinter, 0, 0);
            Set<Tuple> tuples = jedis.zrangeWithScores(zinter, 0, -1);

            List<ActiveLogger> activeLoggers = dsl.select(clickLog, activeLogger).innerJoin(activeLogger)
                    .on(clickLog.imeiMd5.eq(activeLogger.imeiMd5)).where(clickLog.id.in(tuples.stream().map(tuple -> new Double(tuple.getScore()).longValue()).collect(Collectors.toList()))).fetch().stream().map(tuple -> tuple.get(activeLogger).setClickLog(tuple.get(clickLog))).collect(Collectors.toList());

            activeLoggers = activeLoggers.stream()
                    .sorted(Comparator.comparing(ActiveLogger::getActiveTime))
                    .collect(Collectors.collectingAndThen(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(o2 -> gekeyString(key, o2)))), ArrayList::new)
                    );

            handlerFeedback(count, histories, feedbacks, ipuas, key, null, activeLoggers);
        });

        handerResult(histories, feedbacks, ipuas);
//        System.out.println(this.jedis.zrange(getDayCacheRedisKey("feedback"), 0, -1));
        return count.get();
    }

    private void handerResult(List<DayHistory> histories, List<FeedbackLog> feedbacks, List<IpuaNewUser> ipuas) {
        Page.create(feedbacks).forEach(o -> executor.execute(() -> feedbackLogMapper.insertList(o)));
        Page.create(ipuas).forEach(o -> executor.execute(() -> ipuaNewUserMapper.insertList(o)));
        Page.create(histories).forEach(o -> executor.execute(() -> dayHistoryMapper.insertList(o)));
    }

    private String gekeyString(String key, ActiveLogger o2) {
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
            if (!s.equals("imei")) e = e.and(activeLogger.plot.eq(1));
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
            logger.info("开始清理缓存 :{},{}");
            clearCache(null);
            wd.keySet().forEach(s -> getDayCache(s));
            wd.keySet().forEach(s -> logger.info("增加缓存数据量 :{},{}", redisTemplate.opsForZSet().count(getDayCacheRedisKey(s), 0d, Long.MAX_VALUE)));
            logger.info("初始化完成");
        }
        redis.del(getDayCacheRedisKey("sync_active"));
        int sc = etprop.getSc();
        sds = sharding(sc);
    }

    private List<String> sharding(Integer sc) {
        List<Integer> taskList = new ArrayList<>();
        List<String> ret = new ArrayList<>();

        for (int i = 0; i < 60; i++) {
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
