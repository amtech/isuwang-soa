package com.isuwang.dapeng.impl.plugins;

import com.isuwang.dapeng.api.container.*;
import com.isuwang.dapeng.api.plugins.Plugin;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.impl.classloader.AppClassLoader;
import com.isuwang.dapeng.impl.handler.SoaServiceDefinition;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

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
        Map<ProcessorKey, SoaServiceDefinition<?>> processors = new HashMap<>();
        for (ClassLoader appClassLoader : appClassLoaders) {
            try {

                // ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new Object[]{xmlPaths.toArray(new String[0])});
                // context.start();
                Class<?> appClass = appClassLoader.loadClass("org.springframework.context.support.ClassPathXmlApplicationContext");
                Class<?>[] parameterTypes = new Class[]{String[].class};
                Constructor<?> constructor = appClass.getConstructor(parameterTypes);

                Thread.currentThread().setContextClassLoader(appClassLoader);
                Object context = getSpringContext(configPath, appClassLoader,constructor);

                //Start spring context
                Method startMethod = appClass.getMethod("start");
                startMethod.invoke(context);

                //TODO: 需要构造Application对象
                //List<ServiceInfo> appInfos = toApplication(context,appClass);
                Application application = toApplication(context,appClass, processors);

                // IApplication app = new ...
                if (! application.getServiceInfos().isEmpty()) {
                    container.registerApplication(application);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ContainerFactory.getContainer().setServiceProcessors(processors);
    }

    @Override
    public void stop() {

    }


    private Application toApplication(Object context, Class<?> contextClass,Map<ProcessorKey, SoaServiceDefinition<?>> soaProcessors) throws Exception {
        Method method = contextClass.getMethod("getBeansOfType", Class.class);
        Map<String, SoaServiceDefinition<?>> processorMap = (Map<String, SoaServiceDefinition<?>>) method.invoke(context, contextClass.getClassLoader().loadClass(SoaServiceDefinition.class.getName()));

        DapengApplication serviceApplication = new DapengApplication();

        Collection<SoaServiceDefinition<?>> processors = processorMap.values();
        List<ServiceInfo> serviceInfos = new ArrayList<>();
        for (SoaServiceDefinition<?> processor : processors) {
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
                soaProcessors.put(new ProcessorKey(service.name(),service.version()), processor);
            }
            serviceApplication.addServiceInfo(serviceInfo);

        }

        return serviceApplication;
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
