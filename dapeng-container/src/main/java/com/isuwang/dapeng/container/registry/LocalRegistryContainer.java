//package com.isuwang.dapeng.container.registry;
//
//import com.isuwang.dapeng.container.Container;
//import com.isuwang.dapeng.container.spring.SpringContainer;
//import com.isuwang.dapeng.core.ProcessorKey;
//import com.isuwang.dapeng.core.Service;
//import com.isuwang.dapeng.core.SoaServiceDefinition;
//import com.isuwang.dapeng.core.SoaSystemEnvProperties;
//import com.isuwang.dapeng.registry.*;
//import com.isuwang.dapeng.route.Route;
//import com.isuwang.org.apache.thrift.TProcessor;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.lang.reflect.Method;
//import java.util.*;
//
///**
// * Local Registry Container
// *
// * @author craneding
// * @date 16/3/13
// */
//public class LocalRegistryContainer implements Container, RegistryAgent {
//    private static final Logger LOGGER = LoggerFactory.getLogger(LocalRegistryContainer.class);
//
//    @Override
//    public void start() {
//        Map<Object, Class<?>> contexts = SpringContainer.getContexts();
//        Set<Object> ctxs = contexts.keySet();
//
//        for (Object ctx : ctxs) {
//            Class<?> contextClass = contexts.get(ctx);
//
//            try {
//                Method method = contextClass.getMethod("getBeansOfType", Class.class);
//                @SuppressWarnings("unchecked")
//                Map<String, TProcessor<?>> processorMap = (Map<String, TProcessor<?>>) method.invoke(ctx, contextClass.getClassLoader().loadClass(TProcessor.class.getName()));
//
//                Set<String> keys = processorMap.keySet();
//                for (String key : keys) {
//                    TProcessor<?> processor = processorMap.get(key);
//
//                    if (processor.getInterfaceClass().getClass() != null) {
//                        Service service = processor.getInterfaceClass().getAnnotation(Service.class);
//
//                        ProcessorKey processorKey = new ProcessorKey(service.name(), service.version());
//                        getProcessorMap().put(processorKey, processor);
//
//                        this.registerService(service.name(), service.version());
//                    }
//                }
//            } catch (Exception e) {
//                LOGGER.error(e.getMessage(), e);
//            }
//        }
//
//        RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Server, this);
//    }
//
//    @Override
//    public void stop() {
//        ProcessorCache.getProcessorMap().clear();
//    }
//
//    @Override
//    public void registerService(String serverName, String versionName) {
//        LOGGER.info("注册本地服务:{} {}", serverName, versionName);
//    }
//
//    @Override
//    public void registerAllServices() {
//        Set<ProcessorKey> keys = ProcessorCache.getProcessorMap().keySet();
//
//        for (ProcessorKey key : keys) {
//            TProcessor<?> processor = ProcessorCache.getProcessorMap().get(key);
//
//            if (null != processor.getInterfaceClass().getClass()) {
//                Service service = processor.getInterfaceClass().getAnnotation(Service.class);
//
//                this.registerService(service.name(), service.version());
//            }
//        }
//    }
//
//    @Override
//    public void setProcessorMap(Map<ProcessorKey, SoaServiceDefinition<?>> processorMap) {
//    }
//
//    @Override
//    public Map<ProcessorKey, SoaServiceDefinition<?>> getProcessorMap() {
//        return null;
//    }
//
//    @Override
//    public ServiceInfos loadMatchedServices(String serviceName, String versionName, boolean compatible) {
//        final List<ServiceInfo> objects = new ArrayList<>();
//        objects.add(new ServiceInfo("127.0.0.1", SoaSystemEnvProperties.SOA_CONTAINER_PORT, "*"));
//
//        return new ServiceInfos(false, objects);
//    }
//
//
//    @Override
//    public Map<ConfigKey, Object> getConfig(boolean usingFallback, String serviceKey) {
//        return new HashMap<>();
//    }
//
//    @Override
//    public List<Route> getRoutes(boolean usingFallback) {
//        return new ArrayList<>();
//    }
//}
