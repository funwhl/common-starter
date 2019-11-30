package com.eighteen.common.spring.boot.autoconfigure.job;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.dangdang.ddframe.job.executor.handler.JobProperties;
import lombok.Data;

@Data
public class Job {

/*********************DataflowJobConfiguration START********************/

    /**
     * 作业名称
     *
     * @return
     */
    private String jobName;

    /**
     * 作业类型（SIMPLE，DATAFLOW，SCRIPT）
     */
    private String jobType;

    private SimpleJob job;

    /**
     * 任务类路径
     */
    private String jobClass;

    /**
     * cron表达式，用于控制作业触发时间
     *
     * @return
     */
    private String cron;

    /**
     * 作业分片总数
     *
     * @return
     */
    private int shardingTotalCount = 1;

    /**
     * 分片序列号和参数用等号分隔，多个键值对用逗号分隔
     * <p>分片序列号从0开始，不可大于或等于作业分片总数<p>
     * <p>如：<p>
     * <p>0=a,1=b,2=c<p>
     *
     * @return
     */
    private String shardingItemParameters = "";

    /**
     * 作业自定义参数
     * <p>作业自定义参数，可通过传递该参数为作业调度的业务方法传参，用于实现带参数的作业<p>
     * <p>例：每次获取的数据量、作业实例从数据库读取的主键等<p>
     *
     * @return
     */
    private String jobParameter = "";

    /**
     * 是否开启任务执行失效转移，开启表示如果作业在一次任务执行中途宕机，允许将该次未完成的任务在另一作业节点上补偿执行
     *
     * @return
     */
    private boolean failover = false;

    /**
     * 是否开启错过任务重新执行
     *
     * @return
     */
    private boolean misfire = false;

    /**
     * 作业描述信息
     *
     * @return
     */
    private String description = "";

    private boolean overwrite = false;


    /**
     * 是否流式处理数据
     * <p>如果流式处理数据, 则fetchData不返回空结果将持续执行作业<p>
     * <p>如果非流式处理数据, 则处理数据完成后作业结束<p>
     *
     * @return
     */
    private boolean streamingProcess = false;

    /*********************DataflowJobConfiguration END********************/


    /*********************ScriptJobConfiguration START********************/

    /**
     * 脚本型作业执行命令行
     *
     * @return
     */
    private String scriptCommandLine = "";

    /*********************ScriptJobConfiguration END********************/


    /*********************LiteJobConfiguration START********************/

    /**
     * 监控作业运行时状态
     * <p>每次作业执行时间和间隔时间均非常短的情况，建议不监控作业运行时状态以提升效率。<p>
     * <p>因为是瞬时状态，所以无必要监控。请用户自行增加数据堆积监控。并且不能保证数据重复选取，应在作业中实现幂等性。<p>
     * <p>每次作业执行时间和间隔时间均较长的情况，建议监控作业运行时状态，可保证数据不会重复选取。<p>
     *
     * @return
     */
    private boolean monitorExecution = true;

    /**
     * 作业监控端口
     * <p>建议配置作业监控端口, 方便开发者dump作业信息。<p>
     * <p>使用方法: echo “dump” | nc 127.0.0.1 9888<p>
     *
     * @return
     */
    private int monitorPort = -1;

    /**
     * 大允许的本机与注册中心的时间误差秒数
     * <p>如果时间误差超过配置秒数则作业启动时将抛异常<p>
     * <p>配置为-1表示不校验时间误差<p>
     *
     * @return
     */
    private int maxTimeDiffSeconds = -1;

    /**
     * 作业分片策略实现类全路径,默认使用平均分配策略
     *
     * @return
     */
    private String jobShardingStrategyClass = "";

    /**
     * 修复作业服务器不一致状态服务调度间隔时间，配置为小于1的任意值表示不执行修复,单位：分钟
     *
     * @return
     */
    private int reconcileIntervalMinutes = 10;

    /**
     * 作业事件追踪的数据源Bean引用
     *
     * @return
     */
    private String eventTraceRdbDataSource = "";

    /*********************LiteJobConfiguration END********************/

    /**
     * 前置后置任务监听实现类，需实现ElasticJobListener接口
     *
     * @return
     */
    private String listener = "";

    /**
     * 作业是否禁止启动,可用于部署作业时，先禁止启动，部署结束后统一启动
     *
     * @return
     */
    private boolean disabled = false;

    /**
     * 前置后置任务分布式监听实现类，需继承AbstractDistributeOnceElasticJobListener类
     *
     * @return
     */
    private String distributedListener = "";

    /**
     * 最后一个作业执行前的执行方法的超时时间,单位：毫秒
     *
     * @return
     */
    private long startedTimeoutMilliseconds = Long.MAX_VALUE;

    /**
     * 最后一个作业执行后的执行方法的超时时间,单位：毫秒
     *
     * @return
     */
    private long completedTimeoutMilliseconds = Long.MAX_VALUE;

    /**
     * 扩展属性
     */
    private JobProperties jobProperties = new JobProperties();

    Job(String jobName, String jobType, SimpleJob job, String jobClass, String cron, int shardingTotalCount, String shardingItemParameters, String jobParameter, boolean failover, boolean misfire, String description, boolean overwrite, boolean streamingProcess, String scriptCommandLine, boolean monitorExecution, int monitorPort, int maxTimeDiffSeconds, String jobShardingStrategyClass, int reconcileIntervalMinutes, String eventTraceRdbDataSource, String listener, boolean disabled, String distributedListener, long startedTimeoutMilliseconds, long completedTimeoutMilliseconds, JobProperties jobProperties) {
        this.jobName = jobName;
        this.jobType = jobType;
        this.job = job;
        this.jobClass = jobClass;
        this.cron = cron;
        this.shardingTotalCount = shardingTotalCount;
        this.shardingItemParameters = shardingItemParameters;
        this.jobParameter = jobParameter;
        this.failover = failover;
        this.misfire = misfire;
        this.description = description;
        this.overwrite = overwrite;
        this.streamingProcess = streamingProcess;
        this.scriptCommandLine = scriptCommandLine;
        this.monitorExecution = monitorExecution;
        this.monitorPort = monitorPort;
        this.maxTimeDiffSeconds = maxTimeDiffSeconds;
        this.jobShardingStrategyClass = jobShardingStrategyClass;
        this.reconcileIntervalMinutes = reconcileIntervalMinutes;
        this.eventTraceRdbDataSource = eventTraceRdbDataSource;
        this.listener = listener;
        this.disabled = disabled;
        this.distributedListener = distributedListener;
        this.startedTimeoutMilliseconds = startedTimeoutMilliseconds;
        this.completedTimeoutMilliseconds = completedTimeoutMilliseconds;
        this.jobProperties = jobProperties;
    }

    public static JobBuilder builder() {
        return new JobBuilder();
    }

    public static class JobBuilder {
        private String jobName;
        private String jobType;
        private SimpleJob job;
        private String jobClass;
        private String cron;
        private int shardingTotalCount = 1;
        private String shardingItemParameters = "";
        private String jobParameter = "";
        private boolean failover = false;
        private boolean misfire;
        private String description = "";
        private boolean overwrite = true;
        private boolean streamingProcess;
        private String scriptCommandLine;
        private boolean monitorExecution;
        private int monitorPort;
        private int maxTimeDiffSeconds = -1;
        private String jobShardingStrategyClass;
        private int reconcileIntervalMinutes;
        private String eventTraceRdbDataSource;
        private String listener;
        private boolean disabled;
        private String distributedListener;
        private long startedTimeoutMilliseconds;
        private long completedTimeoutMilliseconds;
        private JobProperties jobProperties;

        JobBuilder() {
        }

        public JobBuilder jobName(String jobName) {
            this.jobName = jobName;
            return this;
        }

        public JobBuilder jobType(String jobType) {
            this.jobType = jobType;
            return this;
        }

        public JobBuilder job(SimpleJob job) {
            this.job = job;
            return this;
        }

        public JobBuilder jobClass(String jobClass) {
            this.jobClass = jobClass;
            return this;
        }

        public JobBuilder cron(String cron) {
            this.cron = cron;
            return this;
        }

        public JobBuilder shardingTotalCount(int shardingTotalCount) {
            this.shardingTotalCount = shardingTotalCount;
            return this;
        }

        public JobBuilder shardingItemParameters(String shardingItemParameters) {
            this.shardingItemParameters = shardingItemParameters;
            return this;
        }

        public JobBuilder jobParameter(String jobParameter) {
            this.jobParameter = jobParameter;
            return this;
        }

        public JobBuilder failover(boolean failover) {
            this.failover = failover;
            return this;
        }

        public JobBuilder misfire(boolean misfire) {
            this.misfire = misfire;
            return this;
        }

        public JobBuilder description(String description) {
            this.description = description;
            return this;
        }

        public JobBuilder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public JobBuilder streamingProcess(boolean streamingProcess) {
            this.streamingProcess = streamingProcess;
            return this;
        }

        public JobBuilder scriptCommandLine(String scriptCommandLine) {
            this.scriptCommandLine = scriptCommandLine;
            return this;
        }

        public JobBuilder monitorExecution(boolean monitorExecution) {
            this.monitorExecution = monitorExecution;
            return this;
        }

        public JobBuilder monitorPort(int monitorPort) {
            this.monitorPort = monitorPort;
            return this;
        }

        public JobBuilder maxTimeDiffSeconds(int maxTimeDiffSeconds) {
            this.maxTimeDiffSeconds = maxTimeDiffSeconds;
            return this;
        }

        public JobBuilder jobShardingStrategyClass(String jobShardingStrategyClass) {
            this.jobShardingStrategyClass = jobShardingStrategyClass;
            return this;
        }

        public JobBuilder reconcileIntervalMinutes(int reconcileIntervalMinutes) {
            this.reconcileIntervalMinutes = reconcileIntervalMinutes;
            return this;
        }

        public JobBuilder eventTraceRdbDataSource(String eventTraceRdbDataSource) {
            this.eventTraceRdbDataSource = eventTraceRdbDataSource;
            return this;
        }

        public JobBuilder listener(String listener) {
            this.listener = listener;
            return this;
        }

        public JobBuilder disabled(boolean disabled) {
            this.disabled = disabled;
            return this;
        }

        public JobBuilder distributedListener(String distributedListener) {
            this.distributedListener = distributedListener;
            return this;
        }

        public JobBuilder startedTimeoutMilliseconds(long startedTimeoutMilliseconds) {
            this.startedTimeoutMilliseconds = startedTimeoutMilliseconds;
            return this;
        }

        public JobBuilder completedTimeoutMilliseconds(long completedTimeoutMilliseconds) {
            this.completedTimeoutMilliseconds = completedTimeoutMilliseconds;
            return this;
        }

        public JobBuilder jobProperties(JobProperties jobProperties) {
            this.jobProperties = jobProperties;
            return this;
        }

        public Job build() {
            return new Job(jobName, jobType, job, jobClass, cron, shardingTotalCount, shardingItemParameters, jobParameter, failover, misfire, description, overwrite, streamingProcess, scriptCommandLine, monitorExecution, monitorPort, maxTimeDiffSeconds, jobShardingStrategyClass, reconcileIntervalMinutes, eventTraceRdbDataSource, listener, disabled, distributedListener, startedTimeoutMilliseconds, completedTimeoutMilliseconds, jobProperties);
        }

        public String toString() {
            return "Job.JobBuilder(jobName=" + this.jobName + ", jobType=" + this.jobType + ", job=" + this.job + ", jobClass=" + this.jobClass + ", cron=" + this.cron + ", shardingTotalCount=" + this.shardingTotalCount + ", shardingItemParameters=" + this.shardingItemParameters + ", jobParameter=" + this.jobParameter + ", failover=" + this.failover + ", misfire=" + this.misfire + ", description=" + this.description + ", overwrite=" + this.overwrite + ", streamingProcess=" + this.streamingProcess + ", scriptCommandLine=" + this.scriptCommandLine + ", monitorExecution=" + this.monitorExecution + ", monitorPort=" + this.monitorPort + ", maxTimeDiffSeconds=" + this.maxTimeDiffSeconds + ", jobShardingStrategyClass=" + this.jobShardingStrategyClass + ", reconcileIntervalMinutes=" + this.reconcileIntervalMinutes + ", eventTraceRdbDataSource=" + this.eventTraceRdbDataSource + ", listener=" + this.listener + ", disabled=" + this.disabled + ", distributedListener=" + this.distributedListener + ", startedTimeoutMilliseconds=" + this.startedTimeoutMilliseconds + ", completedTimeoutMilliseconds=" + this.completedTimeoutMilliseconds + ", jobProperties=" + this.jobProperties + ")";
        }
    }
}
