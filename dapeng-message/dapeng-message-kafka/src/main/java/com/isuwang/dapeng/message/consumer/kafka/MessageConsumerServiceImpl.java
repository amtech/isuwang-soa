package com.isuwang.dapeng.message.consumer.kafka;

import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;
import com.isuwang.dapeng.message.consumer.api.service.MessageConsumerService;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by tangliu on 2016/9/12.
 */
public class MessageConsumerServiceImpl implements MessageConsumerService {

    public static Map<String, KafkaConsumer> topicConsumers = new HashMap<>();

    @Override
    public void addConsumer(ConsumerContext context) {

        String groupId = context.getGroupId();
        String topic = context.getTopic();

        Class<?> ifaceClass = context.getIface().getClass();

        try {
            String className = context.getIface() instanceof Proxy ? ((Class) ifaceClass.getMethod("getTargetClass").invoke(context.getIface())).getName() : ifaceClass.getName();
            groupId = "".equals(groupId) ? className : ifaceClass.getName();
            String consumerKey = groupId + ":" + topic;

            if (topicConsumers.containsKey(consumerKey)) {
                topicConsumers.get(consumerKey).addCustomer(context);
            } else {
                KafkaConsumer consumer = new KafkaConsumer(groupId, topic);
                consumer.start();
                consumer.addCustomer(context);
                topicConsumers.put(consumerKey, consumer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
