package com.isuwang.dapeng.spring;

import com.isuwang.dapeng.core.Processor;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.SoaCommonBaseProcessor;
import com.isuwang.org.apache.thrift.TProcessor;
import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * Soa Processor Factory
 *
 * @author craneding
 * @date 16/1/19
 */
public class SoaProcessorFactory implements FactoryBean<TProcessor<?>> {

    private Object serviceRef;
    private String refId;

    public SoaProcessorFactory(Object serviceRef, String refId) {
        this.serviceRef = serviceRef;
        this.refId = refId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public TProcessor<?> getObject() throws Exception {
        final Class<?> aClass = serviceRef.getClass();
        final List<Class<?>> interfaces = Arrays.asList(aClass.getInterfaces());

        List<Class<?>> filterInterfaces = interfaces.stream()
                .filter(anInterface -> anInterface.isAnnotationPresent(Service.class) && anInterface.isAnnotationPresent(Processor.class))
                .map(anInterface -> anInterface)
                .collect(toList());

        if (filterInterfaces.isEmpty())
            throw new RuntimeException("not config @Service & @Processor in " + refId);

        Class<?> interfaceClass = filterInterfaces.get(filterInterfaces.size() - 1);

        Processor processor = interfaceClass.getAnnotation(Processor.class);

        Class<?> processorClass = Class.forName(processor.className(), true, interfaceClass.getClassLoader());
        Constructor<?> constructor = processorClass.getConstructor(interfaceClass);
        TProcessor tProcessor = (TProcessor) constructor.newInstance(serviceRef);

        tProcessor.setInterfaceClass(interfaceClass);

        return tProcessor;
    }

    @Override
    public Class<?> getObjectType() {
        return SoaCommonBaseProcessor.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
