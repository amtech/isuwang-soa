package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.impl.handler.SoaServiceDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DapengApplication implements Application{

    List<ServiceInfo> serviceInfos = new ArrayList<>();

    Map<ProcessorKey, SoaServiceDefinition<?>> processors = new HashMap<>();

    public DapengApplication() {
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
    public ServiceInfo getServiceInfo(String name, String version) {
        ServiceInfo sInfo = null;
        for (ServiceInfo serviceInfo: serviceInfos) {
            if ((serviceInfo.getServiceName() + serviceInfo.getVersion()).equals(name+version)) {
                sInfo = serviceInfo;
            }
        }
        return sInfo;
    }


    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

}
