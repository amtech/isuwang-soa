package com.isuwang.dapeng.message.consumer.kafka;

import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    protected ConsumerConnector consumer;
    protected final static String ZookeeperSessionTimeoutMs = "40000";
    protected final static String ZookeeperSyncTimeMs = "200";
    protected final static String AutoCommitIntervalMs = "1000";

    public void init() {

        logger.info(new StringBuffer("[KafkaConsumer] [init] ")
                .append("zookeeperConnect(").append(zookeeperConnect)
                .append(") groupId(").append(groupId)
                .append(") topic(").append(topic).append(")").toString());

        Properties props = new Properties();
        props.put("zookeeper.connect", zookeeperConnect);
        props.put("group.id", groupId);

        props.put("zookeeper.session.timeout.ms", ZookeeperSessionTimeoutMs);
        props.put("zookeeper.sync.time.ms", ZookeeperSyncTimeMs);
        props.put("auto.commit.interval.ms", AutoCommitIntervalMs);

        consumer = kafka.consumer.Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
    }

    @Override
    public void run() {

        try {
            logger.info("[KafkaConsumer] [run] ");

            Map<String, Integer> topicCountMap = new HashMap<String, Integer>();
            topicCountMap.put(topic, new Integer(1));

            Map<String, List<KafkaStream<byte[], byte[]>>> consumerMap = consumer.createMessageStreams(topicCountMap);
            KafkaStream<byte[], byte[]> stream = consumerMap.get(topic).get(0);

            ConsumerIterator<byte[], byte[]> it = stream.iterator();

            while (it.hasNext()) {
                receive(it.next().message());
            }
        } catch (Exception e) {
            logger.error("[KafkaConsumer] [run] " + e.getMessage(), e);
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
            ConsumerExecutor.doAction(customer, message);
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

}
