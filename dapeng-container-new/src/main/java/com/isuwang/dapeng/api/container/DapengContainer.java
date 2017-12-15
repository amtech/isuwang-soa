package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.api.events.AppEvent;
import com.isuwang.dapeng.api.extension.Dispatcher;
import com.isuwang.dapeng.api.listeners.AppListener;
import com.isuwang.dapeng.api.plugins.Plugin;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.impl.extionsionImpl.ThreadDispatcher;
import com.isuwang.dapeng.impl.extionsionImpl.ThreadPoolDispatcher;
import com.isuwang.dapeng.impl.filters.SharedChain;

import java.util.ArrayList;
import java.util.List;

public class DapengContainer implements Container{

    private List<AppListener> appListeners = new ArrayList<>();
    private List<Application> applications = new ArrayList<>();
    private List<Plugin> plugins = new ArrayList<>();
    public static SharedChain sharedChain;

    @Override
    public void registerAppListener(AppListener listener) {
        this.appListeners.add(listener);
    }

    @Override
    public void unregisterAppListener(AppListener listener) {
        this.appListeners.remove(listener);
    }

    @Override
    public void registerApplication(Application app) {
        this.applications.add(app);
        this.appListeners.forEach(i -> i.appRegistered(new AppEvent(app)));
    }

    @Override
    public void unregisterApplication(Application app) {
        this.applications.remove(app);
        this.appListeners.forEach(i -> i.appUnRegistered(new AppEvent(app)));
    }

    @Override
    public void registerPlugin(Plugin plugin) {
        this.plugins.add(plugin);
    }

    @Override
    public void unregisterPlugin(Plugin plugin) {
        this.plugins.remove(plugin);
    }

    @Override
    public List<Application> getApplications() {
        return this.applications;
    }


    @Override
    public Dispatcher getDispatcher() {
        Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;
        if(useThreadPool){
            return new ThreadPoolDispatcher();
        }else{
            return new ThreadDispatcher();
        }
    }


    @Override
    public SharedChain getSharedChain() {
        //TODO: should return the bean copy..not the real one.

        return this.sharedChain;
    }

    @Override
    public List<Plugin> getPlugins() {
        //TODO: should return the bean copy..not the real one.
        return this.plugins;
    }
}
