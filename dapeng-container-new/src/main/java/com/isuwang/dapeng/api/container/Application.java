package com.isuwang.dapeng.api.container;

import java.util.List;
import java.util.Map;

public interface Application {

    void start();

    void stop();

    List<ServiceInfo> getServiceInfos();

    void addServiceInfos(List<ServiceInfo> serviceInfos);

    void addServiceInfo(ServiceInfo serviceInfo);

    ServiceInfo getServiceInfo(String name, String version);

}
