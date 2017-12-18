package com.isuwang.dapeng.core.container;

import java.util.List;
import java.util.Optional;

public interface Application {

    void start();

    void stop();

    List<ServiceInfo> getServiceInfos();

    void addServiceInfos(List<ServiceInfo> serviceInfos);

    void addServiceInfo(ServiceInfo serviceInfo);

    Optional<ServiceInfo> getServiceInfo(String name, String version);

}
