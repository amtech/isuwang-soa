package com.isuwang.dapeng.core;

import java.util.List;
import java.util.Optional;

public interface Application {

    void start();

    void stop();

    List<ServiceInfo> getServiceInfos();

    void addServiceInfos(List<ServiceInfo> serviceInfos);

    void addServiceInfo(ServiceInfo serviceInfo);

    Optional<ServiceInfo> getServiceInfo(String name, String version);

    default void info(String message) {



    }

    default void error(String message, Throwable exception) {

    }

}
