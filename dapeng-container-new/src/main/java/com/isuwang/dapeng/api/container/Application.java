package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.impl.handler.SoaServiceDefinition;

import java.util.List;
import java.util.Map;

public interface Application {

    void start();

    void stop();

    List<ServiceInfo> getServiceInfos();

    void addServiceInfos(List<ServiceInfo> serviceInfos);

    void addServiceInfo(ServiceInfo serviceInfo);

    ServiceInfo getServiceInfo(String name, String version);

    Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors();

    void setServiceProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors);

    void setServiceProcessor(ProcessorKey processorKey, SoaServiceDefinition<?> processor);

    SoaServiceDefinition<?> getServiceProcessor(ProcessorKey processorKey);

}
