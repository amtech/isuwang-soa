package com.isuwang.dapeng.impl.container;


import com.isuwang.dapeng.core.Application;
import com.isuwang.dapeng.core.ServiceInfo;

import java.util.*;

public class DapengApplication implements Application {

    List<ServiceInfo> serviceInfos;

    public DapengApplication() {
    }

    public DapengApplication(List<ServiceInfo> serviceInfos) {
        this.serviceInfos=Collections.unmodifiableList(serviceInfos);
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
