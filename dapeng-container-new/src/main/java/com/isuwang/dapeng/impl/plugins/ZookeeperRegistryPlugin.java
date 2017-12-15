package com.isuwang.dapeng.impl.plugins;

import com.isuwang.dapeng.api.container.Container;
import com.isuwang.dapeng.api.container.DapengApplication;
import com.isuwang.dapeng.api.container.ServiceInfo;
import com.isuwang.dapeng.api.events.AppEvent;
import com.isuwang.dapeng.api.listeners.AppListener;
import com.isuwang.dapeng.api.plugins.Plugin;

import java.util.List;

public class ZookeeperRegistryPlugin implements AppListener, Plugin {

    final Container container;

    public ZookeeperRegistryPlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);

    }

    @Override
    public void appRegistered(AppEvent event) {

        DapengApplication application = (DapengApplication) event.getSource();
        application.getServiceInfos().forEach(serviceInfo -> {
            registerService(serviceInfo.getServiceName(),serviceInfo.getVersion());
        });

        // Monitor ZK's config properties for service
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        DapengApplication application = (DapengApplication) event.getSource();
        application.getServiceInfos().forEach(serviceInfo -> {
            unRegisterService(serviceInfo.getServiceName(),serviceInfo.getVersion());
        });
    }

    @Override
    public void start() {
        container.getApplications().forEach(app -> {
            List<ServiceInfo> serviceInfos = app.getServiceInfos();
            serviceInfos.forEach(s -> registerService(s.getServiceName(),s.getVersion()));
        });
    }

    @Override
    public void stop() {
        container.getApplications().forEach(app -> {
            List<ServiceInfo> serviceInfos = app.getServiceInfos();
            serviceInfos.forEach(s -> unRegisterService(s.getServiceName(),s.getVersion()));
        });
    }

    public void registerService(String serviceName, String version) {
        System.out.println(" register service: " + serviceName + " " + version);
    }

    public void unRegisterService(String serviceName, String version) {
        System.out.println(" unRegister service: " + serviceName + " " + version);
    }
}
