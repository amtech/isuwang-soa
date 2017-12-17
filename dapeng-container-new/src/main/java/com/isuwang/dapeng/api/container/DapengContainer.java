package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.api.events.AppEvent;
import com.isuwang.dapeng.api.events.AppEventType;
import com.isuwang.dapeng.api.extension.Dispatcher;
import com.isuwang.dapeng.api.listeners.AppListener;
import com.isuwang.dapeng.api.plugins.Plugin;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaServiceDefinition;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.impl.extionsionImpl.ThreadDispatcher;
import com.isuwang.dapeng.impl.extionsionImpl.ThreadPoolDispatcher;
import com.isuwang.dapeng.impl.filters.SharedChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DapengContainer implements Container{

    private static final Logger logger = LoggerFactory.getLogger(DapengContainer.class);
    private List<AppListener> appListeners = new ArrayList<>();
    private List<Application> applications = new ArrayList<>();
    private List<Plugin> plugins = new ArrayList<>();
    private SharedChain sharedChain;
    public Map<ProcessorKey, SoaServiceDefinition<?>> processors;

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
        this.appListeners.forEach(i -> {
            try {
                i.appRegistered(new AppEvent(app, AppEventType.REGISTER));
            } catch (Exception e) {
                logger.error(" Faild to handler appEvent. listener: {}, eventType: {}",i, AppEventType.REGISTER , e.getStackTrace());
            }
        });
    }

    @Override
    public void unregisterApplication(Application app) {
        this.applications.remove(app);
        this.appListeners.forEach(i -> {
            try {
                i.appUnRegistered(new AppEvent(app, AppEventType.UNREGISTER));
            } catch (Exception e) {
                logger.error(" Faild to handler appEvent. listener: {}, eventType: {}",i, AppEventType.UNREGISTER , e.getStackTrace());
            }
        });
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

    public void setSharedChain(SharedChain sharedChain) {
        this.sharedChain = sharedChain;
    }

    @Override
    public SharedChain getSharedChain() {
        //TODO: should return the bean copy..not the real one.

        return sharedChain;
    }

    @Override
    public List<Plugin> getPlugins() {
        //TODO: should return the bean copy..not the real one.
        return this.plugins;
    }

    @Override
    public Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors() {
        return this.processors;
    }

    @Override
    public void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors) {
        if (this.processors == null) {
            this.processors = new HashMap<>();
        }
        this.processors.putAll(processors);
    }

}
