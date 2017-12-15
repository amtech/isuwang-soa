package com.isuwang.dapeng.impl.plugins;


import com.isuwang.dapeng.api.container.Application;
import com.isuwang.dapeng.api.container.Container;
import com.isuwang.dapeng.api.container.ServiceInfo;
import com.isuwang.dapeng.api.events.AppEvent;
import com.isuwang.dapeng.api.listeners.AppListener;
import com.isuwang.dapeng.api.plugins.Plugin;

import java.util.List;

public class TaskSchedulePlugin implements AppListener,Plugin {

    private final Container container;

    public TaskSchedulePlugin(Container container) {
        this.container = container;
        container.registerAppListener(this);
    }


    @Override
    public void appRegistered(AppEvent event) {
        Application application = (Application) event.getSource();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        //TODO: 可以使用Adaptor 或者 Filter 来过滤监听的事件？
        serviceInfos.forEach(i -> runTask(i));
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        Application application = (Application) event.getSource();

        List<ServiceInfo> serviceInfos = application.getServiceInfos();
        //TODO: 可以使用Adaptor 或者 Filter 来过滤监听的事件？
        serviceInfos.forEach(i -> stopTask(i));
    }

    @Override
    public void start() {
        container.getApplications().forEach(i -> {
            List<ServiceInfo> serviceInfos = i.getServiceInfos();
            serviceInfos.forEach(s -> runTask(s));
        });
    }

    @Override
    public void stop() {

    }

    public void runTask(ServiceInfo appInfo) {
        //Some logic
    }


    public void stopTask(ServiceInfo appInfo) {
        //Some logic
    }
}
