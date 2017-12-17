package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaServiceDefinition;

import java.util.*;

public class DapengApplication implements Application{

    List<ServiceInfo> serviceInfos = new ArrayList<>();

    Map<ProcessorKey, SoaServiceDefinition<?>> processors = new HashMap<>();

    public DapengApplication() {
    }

    public DapengApplication(List<ServiceInfo> serviceInfos) {
        this.serviceInfos.addAll(serviceInfos);
    }

    @Override
    public List<ServiceInfo> getServiceInfos() {
        return this.serviceInfos;
    }

    @Override
    public void addServiceInfos(List<ServiceInfo> serviceInfos) {
        this.serviceInfos.addAll(serviceInfos);
    }

    @Override
    public void addServiceInfo(ServiceInfo serviceInfo) {
        this.serviceInfos.add(serviceInfo);
    }

    @Override
    public Optional<ServiceInfo> getServiceInfo(String name, String version) {
        return serviceInfos.stream().filter(i -> name.equals(i.getServiceName()) && version.equals(i.getVersion())).findFirst();
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}
