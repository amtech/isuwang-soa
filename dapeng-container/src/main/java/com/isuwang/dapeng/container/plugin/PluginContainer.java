package com.isuwang.dapeng.container.plugin;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.container.spring.SpringContainer;
import com.isuwang.dapeng.core.plugin.SoaPluginContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;
import java.util.ServiceLoader;

/**
 * Created by tangliu on 2016/9/18.
 */
public class PluginContainer implements Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginContainer.class);

    @Override
    @SuppressWarnings("unchecked")
    public void start() {

        for (Map.Entry<Object, Class<?>> entry : SpringContainer.getContexts().entrySet()) {
            SoaPluginContainer.contexts.put(entry.getKey(),entry.getValue());
        }

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ServiceLoader<SoaPluginContainer>soaPluginContainers = ServiceLoader.load(SoaPluginContainer.class,contextClassLoader);
        for (SoaPluginContainer soaPluginContainer:soaPluginContainers) {
            soaPluginContainer.start();
            System.out.println("load plugin container:" + soaPluginContainer.getClass().getName());
        }

        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Override
    public void stop() {
    }

}
