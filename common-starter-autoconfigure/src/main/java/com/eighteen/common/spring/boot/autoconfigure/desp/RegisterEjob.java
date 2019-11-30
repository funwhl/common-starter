package com.eighteen.common.spring.boot.autoconfigure.desp;//package com.eighteen.common.spring.boot.autoconfigure.job;
//
//import com.dangdang.ddframe.job.api.ElasticJob;
//import com.dangdang.ddframe.job.config.JobCoreConfiguration;
//import com.dangdang.ddframe.job.config.simple.SimpleJobConfiguration;
//import com.dangdang.ddframe.job.lite.config.LiteJobConfiguration;
//import com.dangdang.ddframe.job.lite.spring.api.SpringJobScheduler;
//import com.dangdang.ddframe.job.reg.zookeeper.ZookeeperRegistryCenter;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.beans.BeansException;
//import org.springframework.beans.factory.InitializingBean;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.ApplicationContextAware;
//import org.springframework.core.annotation.AnnotationUtils;
//import org.springframework.stereotype.Component;
//
//import java.util.Map;
//import java.util.Optional;
//
///**
// * Created by eighteen.
// * Date: 2019/8/22
// * Time: 20:54
// */
//
//public class RegisterEjob {
//
//    private ZookeeperRegistryCenter zookeeperRegistryCenter;
//
//    private ApplicationContext applicationContext;
//
//    public RegisterEjob(ZookeeperRegistryCenter zookeeperRegistryCenter, ApplicationContext applicationContext) {
//        this.zookeeperRegistryCenter = zookeeperRegistryCenter;
//        this.applicationContext = applicationContext;
//    }
//
//
//    public void init() {
//        System.out.println("11111111111111111111111111111111111111111111111111");
//        Map<String,Object> registerJobs = applicationContext.getBeansWithAnnotation(TaskJob.class);
//        for(Map.Entry<String,Object> entry : registerJobs.entrySet()){
//            try {
//                Object object = entry.getValue();
//                if(! (object instanceof ElasticJob)){
//                    throw new ClassCastException("["+object.getClass().getName() + "] The class type is not com.dangdang.ddframe.job.api.ElasticJob");
//                }
//                TaskJob taskJob = AnnotationUtils.findAnnotation(object.getClass(),TaskJob.class);
//                SpringJobScheduler springJobScheduler = new SpringJobScheduler(
//                        (ElasticJob) object,
//                        zookeeperRegistryCenter,
//                        getJobConfiguration(taskJob,object)
//                );
//                springJobScheduler.init();
//            }catch (Exception e){
//                System.out.println("注册任务异常 ");
//            }
//        }
//    }
//
//    private LiteJobConfiguration getJobConfiguration(TaskJob taskJob, Object object) {
//
//        Optional.ofNullable(taskJob.jobName()).orElseThrow(() -> new IllegalArgumentException("The jobName cannot be null !"));
//        Optional.ofNullable(taskJob.cron()).orElseThrow(() -> new IllegalArgumentException("The cron cannot be null !"));
//        Optional.ofNullable(taskJob.desc()).orElseThrow(() -> new IllegalArgumentException("The desc cannot be null !"));
//
//        SimpleJobConfiguration simpleJobConfiguration = new SimpleJobConfiguration(
//                JobCoreConfiguration
//                        .newBuilder(taskJob.jobName(), taskJob.cron(),taskJob.shardingTotalCount())
//                        .shardingItemParameters(StringUtils.isEmpty(taskJob.shardingItemParameters()) ? null : taskJob.shardingItemParameters())
//                        .description(taskJob.desc())
//                        .failover(taskJob.failover())
//                        .jobParameter(StringUtils.isEmpty(taskJob.jobParameter()) ? null : taskJob.jobParameter())
//                        .build(),
//                object.getClass().getName());
//
//        LiteJobConfiguration liteJobConfiguration = LiteJobConfiguration
//                .newBuilder(simpleJobConfiguration).overwrite(taskJob.overwrite())
//                .maxTimeDiffSeconds(10)
//                .monitorExecution(true)
//                .build();
//        return liteJobConfiguration;
//    }
//
//}
