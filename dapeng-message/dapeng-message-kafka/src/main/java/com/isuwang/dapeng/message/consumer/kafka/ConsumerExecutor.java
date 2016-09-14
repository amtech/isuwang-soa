package com.isuwang.dapeng.message.consumer.kafka;

import com.isuwang.dapeng.core.SoaProcessFunction;
import com.isuwang.dapeng.core.TBeanSerializer;
import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tangliu on 2016/8/4.
 */
public class ConsumerExecutor {

    private static final Logger logger = LoggerFactory.getLogger("container.kafka.consumer");

    private static ExecutorService executorService = Executors.newFixedThreadPool(10, new DefaultThreadFactory("MessageConsumerExecutor"));

    public static void doAction(ConsumerContext customer, byte[] message) {
        executorService.execute(() -> work(customer, message));
    }

    public static void work(ConsumerContext customer, byte[] message) {

        SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>> soaProcessFunction = customer.getSoaProcessFunction();
        Object iface = customer.getIface();

        Object args = soaProcessFunction.getEmptyArgsInstance();
        Field field = args.getClass().getDeclaredFields()[0];
        field.setAccessible(true);//暴力访问，取消私有权限,让对象可以访问

        ByteBuffer buf = ByteBuffer.wrap(message);
        try {
            field.set(args, buf);

            logger.info("{}收到kafka消息，执行{}方法", iface.getClass().getName(), soaProcessFunction.getMethodName());
            soaProcessFunction.getResult(iface, args);
            logger.info("{}收到kafka消息，执行{}方法完成", iface.getClass().getName(), soaProcessFunction.getMethodName());
        } catch (Exception e) {
            logger.error("{}收到kafka消息，执行{}方法异常", iface.getClass().getName(), soaProcessFunction.getMethodName());
            logger.error(e.getMessage(), e);
        }
    }

    public static class DefaultThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String name;

        public DefaultThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" + threadNumber.getAndIncrement());
        }
    }
}
