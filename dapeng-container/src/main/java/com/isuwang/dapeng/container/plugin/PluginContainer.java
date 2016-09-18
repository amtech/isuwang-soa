package com.isuwang.dapeng.container.plugin;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.container.spring.SpringContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by tangliu on 2016/9/18.
 */
public class PluginContainer implements Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(PluginContainer.class);

    @Override
    @SuppressWarnings("unchecked")
    public void start() {

        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

        for (ClassLoader pluginClassLoader : SpringContainer.pluginClassLoaders) {
            try {
//               pluginClassLoader.getResourceAsStream("container.xml");
                String containerRef = "com.isuwang.dapeng.message.consumer.container.KafkaMessageContainer";
                Class<?> containerClass = pluginClassLoader.loadClass(containerRef);

                Field contextField = containerClass.getField("contexts");
                contextField.set(containerClass, SpringContainer.getContexts());

                Object containerObj = containerClass.newInstance();

                Thread.currentThread().setContextClassLoader(pluginClassLoader);
                Method startMethod = containerClass.getMethod("start");
                startMethod.invoke(containerObj);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    @Override
    public void stop() {
    }

}
