package com.isuwang.dapeng.container.timer;

import com.isuwang.dapeng.container.util.LoggerUtil;
import com.isuwang.dapeng.core.SoaProcessFunction;
import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.dapeng.core.helper.MasterHelper;
import com.isuwang.org.apache.thrift.TException;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tangliu on 2016/8/17.
 */
public class ScheduledJob implements Job {

    Logger logger = LoggerFactory.getLogger(LoggerUtil.SCHEDULED_TASK);

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDataMap data = context.getJobDetail().getJobDataMap();
        String serviceName = data.getString("serviceName");
        String versionName = data.getString("versionName");

        if (!MasterHelper.isMaster(serviceName, versionName)) {
            logger.info("--定时任务({}:{})不是Master，跳过--", serviceName, versionName);
            return;
        }

        logger.info("定时任务({})开始执行", context.getJobDetail().getKey().getName());

        SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction =
                (SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>>) data.get("function");

        Object iface = data.get("iface");
        Object args = soaProcessFunction.getEmptyArgsInstance();

        try {
            soaProcessFunction.getResult(iface, args);
            logger.info("定时任务({})执行完成", context.getJobDetail().getKey().getName());
        } catch (TException e) {
            logger.error("定时任务({})执行异常", context.getJobDetail().getKey().getName());
            logger.error(e.getMessage(), e);
        }

    }
}
