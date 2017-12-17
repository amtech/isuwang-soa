package com.isuwang.dapeng.impl.plugins;

import com.isuwang.dapeng.api.container.*;
import com.isuwang.dapeng.api.plugins.Plugin;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.SoaServiceDefinition;
import com.isuwang.dapeng.impl.classloader.AppClassLoader;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class SpringAppLoader implements Plugin {

    private final Container container;
    private final List<AppClassLoader> appClassLoaders;

    public SpringAppLoader(Container container, List<AppClassLoader> appClassLoaders) {
        this.container = container;
        this.appClassLoaders = appClassLoaders;
    }

    //伪代码
    @Override
    public void start() {
        String configPath = "META-INF/spring/services.xml";

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (ClassLoader appClassLoader : appClassLoaders) {
            try {

                // ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new Object[]{xmlPaths.toArray(new String[0])});
                // context.start();
                Class<?> appClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext");
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Constructor<?> constructor = appClass.getConstructor(parameterTypes);

                Thread.currentThread().setContextClassLoader(appClassLoader);
                Object context = getSpringContext(configPath, appClassLoader,constructor);

                Method method = appClass.getMethod("getBeansOfType", Class.class);

                Map<String, SoaServiceDefinition<?>> processorMap = (Map<String, SoaServiceDefinition<?>>)
                        method.invoke(context, appClassLoader.loadClass(SoaServiceDefinition.class.getName()));
                //TODO: 需要构造Application对象
                Map<String,ServiceInfo> appInfos = toServiceInfos(processorMap);
                Application application = new DapengApplication(appInfos.values().stream().collect(Collectors.toList()));

                Map<ProcessorKey, SoaServiceDefinition<?>> serviceDefinitionMap = toSoaServiceDefinitionMap(appInfos,processorMap);
                container.registerAppProcessors(serviceDefinitionMap);

                // IApplication app = new ...
                if (! application.getServiceInfos().isEmpty()) {
                    container.registerApplication(application);
                }

                //Start spring context
                Method startMethod = appClass.getMethod("start");
                startMethod.invoke(context);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {

    }

    private Map<String,ServiceInfo> toServiceInfos(Map<String, SoaServiceDefinition<?>> processorMap) throws Exception {

        Map<String,ServiceInfo> serviceInfoMap = new HashMap<>();
        for (Map.Entry<String, SoaServiceDefinition<?>> processorEntry : processorMap.entrySet()) {
            String processorKey = processorEntry.getKey();
            SoaServiceDefinition<?> processor = processorEntry.getValue();
            ServiceInfo serviceInfo = new ServiceInfo();
            long count = new ArrayList<>(Arrays.asList(processor.getIface().getClass().getInterfaces()))
                    .stream()
                    .filter(m -> m.getName().equals("org.springframework.aop.framework.Advised"))
                    .count();

            Class<?> ifaceClass = (Class) (count > 0 ? processor.getIface().getClass().getMethod("getTargetClass").invoke(processor.getIface()) : processor.getIface().getClass());
            serviceInfo.setIfaceClass(ifaceClass);
            if (processor.getIface().getClass() != null) {
                Service service = processor.getIfaceClass().getAnnotation(Service.class);
                serviceInfo.setServiceName(service.name());
                serviceInfo.setVersion(service.version());
            }
            serviceInfoMap.put(processorKey,serviceInfo);
        }

        return serviceInfoMap;
    }



    private Map<ProcessorKey, SoaServiceDefinition<?>> toSoaServiceDefinitionMap(Map<String,ServiceInfo> serviceInfoMap, Map<String, SoaServiceDefinition<?>> processorMap) {
        Map<ProcessorKey, SoaServiceDefinition<?>>  serviceDefinitions = new HashMap<>();
        serviceInfoMap.entrySet().forEach(i -> {
            serviceDefinitions.put(new ProcessorKey(i.getValue().getServiceName(),i.getValue().getVersion()), processorMap.get(i.getKey()));
        });
        return serviceDefinitions;
    }


    private Object getSpringContext(String configPath, ClassLoader appClassLoader, Constructor<?> constructor) throws Exception{
        List<String> xmlPaths = new ArrayList<>();
        File file = new File(configPath);

        Enumeration<URL> resources = appClassLoader.getResources(configPath);

        while (resources.hasMoreElements()) {
            URL nextElement = resources.nextElement();
            // not load isuwang-soa-transaction-impl
            if (!nextElement.getFile().matches(".*dapeng-transaction-impl.*")) {
                xmlPaths.add(nextElement.toString());
            }
        }
        Object context = constructor.newInstance(new Object[]{xmlPaths.toArray(new String[0])});
        return context;
    }

}
