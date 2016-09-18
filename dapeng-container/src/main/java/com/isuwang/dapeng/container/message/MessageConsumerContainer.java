//package com.isuwang.dapeng.container.message;
//
//import com.isuwang.dapeng.container.Container;
//import com.isuwang.dapeng.container.spring.SpringContainer;
//import com.isuwang.dapeng.core.SoaBaseProcessor;
//import com.isuwang.dapeng.core.SoaProcessFunction;
//import com.isuwang.dapeng.core.TBeanSerializer;
//import com.isuwang.dapeng.message.consumer.api.context.ConsumerContext;
//import com.isuwang.dapeng.message.consumer.api.service.MessageConsumerService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.annotation.Annotation;
//import java.lang.reflect.Method;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Map;
//import java.util.Set;
//
///**
// * Message Consumer Container
// *
// * @author tangliu
// * @date 16/8/3
// */
//public class MessageConsumerContainer implements Container {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerContainer.class);
//
//    @Override
//    @SuppressWarnings("unchecked")
//    public void start() {
//
//        MessageConsumerService consumerService;
//        //实例化bean
//        try {
//            Class<?> consumerClass = MessageConsumerContainer.class.getClassLoader().loadClass("com.isuwang.dapeng.message.consumer.kafka.MessageConsumerServiceImpl");
//            consumerService = (MessageConsumerService) consumerClass.newInstance();
//        } catch (Exception e) {
//            LOGGER.error("MessageConsumerService加载失败", e);
//            return;
//        }
//
//        Map<Object, Class<?>> contexts = SpringContainer.getContexts();
//        Set<Object> ctxs = contexts.keySet();
//
//        for (Object ctx : ctxs) {
//
//            Class<?> contextClass = contexts.get(ctx);
//
//            try {
//                Method getBeansOfType = contextClass.getMethod("getBeansOfType", Class.class);
//                Map<String, SoaBaseProcessor<?>> processorMap = (Map<String, SoaBaseProcessor<?>>) getBeansOfType.invoke(ctx, contextClass.getClassLoader().loadClass(SoaBaseProcessor.class.getName()));
//
//                Set<String> keys = processorMap.keySet();
//                for (String key : keys) {
//                    SoaBaseProcessor<?> processor = processorMap.get(key);
//
//                    long count = new ArrayList<>(Arrays.asList(processor.getIface().getClass().getInterfaces()))
//                            .stream()
//                            .filter(m -> m.getName().equals("org.springframework.aop.framework.Advised"))
//                            .count();
//
//                    Class<?> ifaceClass = (Class) (count > 0 ? processor.getIface().getClass().getMethod("getTargetClass").invoke(processor.getIface()) : processor.getIface().getClass());
//
//                    Class MessageConsumerClass = ifaceClass.getClassLoader().loadClass("com.isuwang.dapeng.message.consumer.api.annotation.MessageConsumer");
//                    Class MessageConsumerActionClass = ifaceClass.getClassLoader().loadClass("com.isuwang.dapeng.message.consumer.api.annotation.MessageConsumerAction");
//
//                    if (ifaceClass.isAnnotationPresent(MessageConsumerClass)) {
//
//                        Annotation messageConsumer = ifaceClass.getAnnotation(MessageConsumerClass);
//                        String groupId = (String) messageConsumer.getClass().getDeclaredMethod("groupId").invoke(messageConsumer);
//
//                        for (Method method : ifaceClass.getMethods()) {
//                            if (method.isAnnotationPresent(MessageConsumerActionClass)) {
//
//                                String methodName = method.getName();
//                                SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>> soaProcessFunction = (SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>>) processor.getProcessMapView().get(methodName);
//
//                                Annotation annotation = method.getAnnotation(MessageConsumerActionClass);
//                                String topic = (String) annotation.getClass().getDeclaredMethod("topic").invoke(annotation);
//
//                                ConsumerContext consumerContext = new ConsumerContext();
//                                consumerContext.setGroupId(groupId);
//                                consumerContext.setTopic(topic);
//                                consumerContext.setIface(processor.getIface());
//                                consumerContext.setSoaProcessFunction(soaProcessFunction);
//
//                                consumerService.addConsumer(consumerContext);
//
//                                LOGGER.info("添加消息订阅({})({})", ifaceClass.getName(), method.getName());
//                            }
//                        }
//                    }
//                }
//            } catch (Exception e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//        }
//    }
//
//    @Override
//    public void stop() {
//    }
//
//
//}
