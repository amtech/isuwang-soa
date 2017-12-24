package com.isuwang.dapeng.container.timer;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.container.spring.SpringContainer;
import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.timer.ScheduledTask;
import com.isuwang.dapeng.core.timer.ScheduledTaskCron;
import com.isuwang.org.apache.thrift.TProcessor;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.triggers.CronTriggerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Created by tangliu on 2016/8/17.
 * <p>
 * 定时任务，使用Quartz实现
 */
public class ScheduledTaskContainer implements Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduledTaskContainer.class);

    Scheduler scheduler = null;

    @Override
    @SuppressWarnings("unchecked")
    public void start() {

        Map<Object, Class<?>> contexts = SpringContainer.getContexts();
        Set<Object> ctxs = contexts.keySet();

        for (Object ctx : ctxs) {

            Class<?> contextClass = contexts.get(ctx);

            try {
                Method getBeansOfType = contextClass.getMethod("getBeansOfType", Class.class);
                Map<String, TProcessor<?>> processorMap = (Map<String, TProcessor<?>>) getBeansOfType.invoke(ctx, contextClass.getClassLoader().loadClass(TProcessor.class.getName()));

                Set<String> keys = processorMap.keySet();
                for (String key : keys) {
                    TProcessor<?> processor = processorMap.get(key);

                    long count = new ArrayList<>(Arrays.asList(processor.getIface().getClass().getInterfaces()))
                            .stream()
                            .filter(m -> m.getName().equals("org.springframework.aop.framework.Advised"))
                            .count();

                    Class<?> ifaceClass = (Class) (count > 0 ? processor.getIface().getClass().getMethod("getTargetClass").invoke(processor.getIface()) : processor.getIface().getClass());

                    if (ifaceClass.isAnnotationPresent(ScheduledTask.class)) {

                        for (Method method : ifaceClass.getMethods()) {
                            if (method.isAnnotationPresent(ScheduledTaskCron.class)) {

                                String methodName = method.getName();
                                SoaProcessFunction<Object, Object, Object, ? extends BeanSerializer<Object>, ? extends BeanSerializer<Object>> soaProcessFunction = (SoaProcessFunction<Object, Object, Object, ? extends BeanSerializer<Object>, ? extends BeanSerializer<Object>>) processor.getProcessMapView().get(methodName);

                                ScheduledTaskCron cron = method.getAnnotation(ScheduledTaskCron.class);
                                String cronStr = cron.cron();

                                Service service = processor.getInterfaceClass().getAnnotation(Service.class);
                                String serviceName = service.name();
                                String versionName = service.version();

                                //new quartz job
                                JobDataMap jobDataMap = new JobDataMap();
                                jobDataMap.put("function", soaProcessFunction);
                                jobDataMap.put("iface", processor.getIface());
                                jobDataMap.put("serviceName", serviceName);
                                jobDataMap.put("versionName", versionName);
                                JobDetail job = JobBuilder.newJob(ScheduledJob.class).withIdentity(ifaceClass.getName() + ":" + methodName).setJobData(jobDataMap).build();

                                CronTriggerImpl trigger = new CronTriggerImpl();
                                trigger.setName(job.getKey().getName());
                                trigger.setJobKey(job.getKey());
                                try {
                                    trigger.setCronExpression(cronStr);
                                } catch (ParseException e) {
                                    LOGGER.error("定时任务({}:{})Cron解析出错", ifaceClass.getName(), methodName);
                                    LOGGER.error(e.getMessage(), e);
                                    continue;
                                }

                                if (scheduler == null) {
                                    try {
                                        scheduler = StdSchedulerFactory.getDefaultScheduler();
                                        scheduler.start();
                                    } catch (SchedulerException e) {
                                        LOGGER.error("ScheduledTaskContainer启动失败");
                                        LOGGER.error(e.getMessage(), e);
                                        return;
                                    }
                                }
                                scheduler.scheduleJob(job, trigger);
                                LOGGER.info("添加定时任务({}:{})成功", ifaceClass.getName(), methodName);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {

        try {
            if (scheduler != null)
                scheduler.shutdown(true);
        } catch (SchedulerException e) {
            LOGGER.error(e.getMessage(), e);
        }
        scheduler = null;
    }

}