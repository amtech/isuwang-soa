package com.isuwang.dapeng.message.consumer.container;

import com.isuwang.dapeng.api.ContainerFactory;
import com.isuwang.dapeng.api.Plugin;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.definition.SoaFunctionDefinition;
import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;
import com.isuwang.dapeng.message.consumer.api.service.MessageConsumerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Created by tangliu on 2016/9/18.
 */
public class KafkaMessagePlugin implements Plugin{

    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaMessagePlugin.class);
    private MessageConsumerService consumerService = new com.isuwang.dapeng.message.consumer.kafka.MessageConsumerServiceImpl();

    @SuppressWarnings("unchecked")
    @Override
    public void start() {

        Map<ProcessorKey, SoaServiceDefinition<?>> processorMap = ContainerFactory.getContainer().getServiceProcessors();
        try {
            Collection<SoaServiceDefinition<?>> soaServiceDefinitions = processorMap.values();
            for (SoaServiceDefinition definition : soaServiceDefinitions) {
                Class<?> ifaceClass = definition.ifaceClass;
                Class MessageConsumerClass = null;
                Class MessageConsumerActionClass = null;
                try {
                    MessageConsumerClass = ifaceClass.getClassLoader().loadClass("com.isuwang.dapeng.message.consumer.api.annotation.MessageConsumer");
                    MessageConsumerActionClass = ifaceClass.getClassLoader().loadClass("com.isuwang.dapeng.message.consumer.api.annotation.MessageConsumerAction");
                } catch (ClassNotFoundException e) {
                    LOGGER.info("({})添加消息订阅失败:{}", ifaceClass.getName(), e.getMessage());
                    break;
                }

                if (ifaceClass.isAnnotationPresent(MessageConsumerClass)) {

                    Annotation messageConsumer = ifaceClass.getAnnotation(MessageConsumerClass);
                    String groupId = (String) messageConsumer.getClass().getDeclaredMethod("groupId").invoke(messageConsumer);

                    for (Method method : ifaceClass.getMethods()) {
                        if (method.isAnnotationPresent(MessageConsumerActionClass)) {

                            String methodName = method.getName();

                            Annotation annotation = method.getAnnotation(MessageConsumerActionClass);
                            String topic = (String) annotation.getClass().getDeclaredMethod("topic").invoke(annotation);
                            SoaFunctionDefinition functionDefinition = (SoaFunctionDefinition)definition.functions.get(methodName);

                            ConsumerContext consumerContext = new ConsumerContext();
                            consumerContext.setGroupId(groupId);
                            consumerContext.setTopic(topic);
                            consumerContext.setIface(definition.iface);
                            consumerContext.setSoaFunctionDefinition(functionDefinition);

                            consumerService.addConsumer(consumerContext);

                            LOGGER.info("添加消息订阅({})({})", ifaceClass.getName(), method.getName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        consumerService.clearConsumers();
    }
}
