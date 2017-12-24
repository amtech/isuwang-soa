package com.isuwang.dapeng.container.registry;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.container.spring.SpringContainer;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.log.SoaAppClassLoaderCache;
import com.isuwang.dapeng.registry.RegistryAgent;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.dapeng.registry.zookeeper.RegistryAgentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * Registry Container
 *
 * @author craneding
 * @date 16/1/19
 */
public class ZookeeperRegistryContainer implements Container {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperRegistryContainer.class);

    private final RegistryAgent registryAgent = new RegistryAgentImpl(false);

    @Override
    @SuppressWarnings("unchecked")
    public void start() {
        RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Server, registryAgent);

        //registryAgent.setProcessorMap(ContainerFactory);
        registryAgent.start();

        Map<Object, Class<?>> contexts = SpringContainer.getContexts();
        Map<Object,ClassLoader>appClassLoaderMap = SpringContainer.getClassLoaderMap();
        Set<Object> ctxs = contexts.keySet();

        for (Object ctx : ctxs) {
            Class<?> contextClass = contexts.get(ctx);

            try {
                Method method = contextClass.getMethod("getBeansOfType", Class.class);
                Map<String, TProcessor<?>> processorMap = (Map<String, TProcessor<?>>) method.invoke(ctx, contextClass.getClassLoader().loadClass(TProcessor.class.getName()));

                Set<String> keys = processorMap.keySet();
                for (String key : keys) {
                    TProcessor<?> processor = processorMap.get(key);

                    if (processor.getInterfaceClass().getClass() != null) {
                        Service service = processor.getInterfaceClass().getAnnotation(Service.class);

                        ProcessorKey processorKey = new ProcessorKey(service.name(), service.version());
                        ProcessorCache.getProcessorMap().put(processorKey, processor);
                        registryAgent.registerService(service.name(), service.version());
                        SoaAppClassLoaderCache.getAppClassLoaderMap().put(processorKey,appClassLoaderMap.get(ctx));
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {
        ProcessorCache.getProcessorMap().clear();

        registryAgent.stop();
    }

    public static Map<ProcessorKey, TProcessor<?>> getProcessorMap() {
        return ProcessorCache.getProcessorMap();
    }

}
