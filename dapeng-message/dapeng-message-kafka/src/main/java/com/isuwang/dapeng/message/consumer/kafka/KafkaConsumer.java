package com.isuwang.dapeng.message.consumer.kafka;

import com.isuwang.dapeng.core.SoaProcessFunction;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by tangliu on 2016/8/3.
 */
public class KafkaConsumer extends Thread {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    private List<ConsumerContext> customers = new ArrayList<>();

    private String groupId, topic;

    public KafkaConsumer(String groupId, String topic) {
        this.groupId = groupId;
        this.topic = topic;
        init();
    }

    private String zookeeperConnect = SoaSystemEnvProperties.SOA_ZOOKEEPER_KAFKA_HOST;

    protected org.apache.kafka.clients.consumer.KafkaConsumer<byte[], byte[]> consumer;
//    protected final static String ZookeeperSessionTimeoutMs = "40000";
//    protected final static String ZookeeperSyncTimeMs = "200";
//    protected final static String AutoCommitIntervalMs = "1000";

    public void init() {

        logger.info(new StringBuffer("[KafkaConsumer] [init] ")
                .append("zookeeperConnect(").append(zookeeperConnect)
                .append(") groupId(").append(groupId)
                .append(") topic(").append(topic).append(")").toString());

        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeperConnect);
        props.put("group.id", groupId);
        props.put("enable.auto.commit", "true");
        props.put("auto.commit.interval.ms", "1000");

//        props.put("zookeeper.session.timeout.ms", ZookeeperSessionTimeoutMs);
//        props.put("zookeeper.sync.time.ms", ZookeeperSyncTimeMs);
//        props.put("auto.commit.interval.ms", AutoCommitIntervalMs);
        consumer = new org.apache.kafka.clients.consumer.KafkaConsumer<>(props);

    }

    @Override
    public void run() {

        try {
            logger.info("[KafkaConsumer][{}][run] ", groupId + ":" + topic);

            consumer.subscribe(Arrays.asList(topic));
            while (true) {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(100);
                for (ConsumerRecord<byte[], byte[]> record : records){
                    receive(record.value());
                    System.out.printf("offset = %d, key = %s, value = %s%n", record.offset(), record.key(), record.value());}
            }

        } catch (Exception e) {
            logger.error("[KafkaConsumer][{}][run] " + e.getMessage(), groupId + ":" + topic, e);
        }
    }


    /**
     * Kafka Consumer接收到消息，调用方法消费消息
     *
     * @param message
     */
    private void receive(byte[] message) {

        logger.info("KafkaConsumer groupId({}) topic({}) 收到消息", groupId, topic);
        for (ConsumerContext customer : customers) {
           dealMessage(customer, message);
        }
    }

    /**
     * 添加一个订阅同一个topic的“客户端”,客户端可以理解为一个订阅消息的方法
     *
     * @param client
     */
    public void addCustomer(ConsumerContext client) {
        this.customers.add(client);
    }

    public List<ConsumerContext> getCustomers() {
        return customers;
    }

    public void setCustomers(List<ConsumerContext> customers) {
        this.customers = customers;
    }


    private void dealMessage(ConsumerContext customer, byte[] message) {

        SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction = customer.getSoaProcessFunction();
        Object iface = customer.getIface();

        long count = new ArrayList<>(Arrays.asList(iface.getClass().getInterfaces()))
                .stream()
                .filter(m -> m.getName().equals("org.springframework.aop.framework.Advised"))
                .count();

        Class<?> ifaceClass;
        try {
            ifaceClass = (Class) (count > 0 ? iface.getClass().getMethod("getTargetClass").invoke(iface) : iface.getClass());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            ifaceClass = iface.getClass();
        }

        Object args = soaProcessFunction.getEmptyArgsInstance();
        Field field = args.getClass().getDeclaredFields()[0];
        field.setAccessible(true);//暴力访问，取消私有权限,让对象可以访问

        ByteBuffer buf = ByteBuffer.wrap(message);
        try {
            field.set(args, buf);

            logger.info("{}收到kafka消息，执行{}方法", ifaceClass.getName(), soaProcessFunction.getMethodName());
            soaProcessFunction.getResult(iface, args);
            logger.info("{}收到kafka消息，执行{}方法完成", ifaceClass.getName(), soaProcessFunction.getMethodName());
        } catch (Exception e) {
            logger.error("{}收到kafka消息，执行{}方法异常", ifaceClass.getName(), soaProcessFunction.getMethodName());
            logger.error(e.getMessage(), e);
        }
    }

}
