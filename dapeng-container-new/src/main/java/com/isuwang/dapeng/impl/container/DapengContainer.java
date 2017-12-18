package com.isuwang.dapeng.impl.container;

import com.isuwang.dapeng.api.*;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaServiceDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DapengContainer implements Container {

    private static final Logger logger = LoggerFactory.getLogger(DapengContainer.class);
    private List<AppListener> appListeners = new Vector<>();
    private List<Application> applications = new Vector<>();
    private List<Plugin> plugins = new ArrayList<>();
    private SharedChain sharedChain;
    public Map<ProcessorKey, SoaServiceDefinition<?>> processors = new ConcurrentHashMap<>();

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


    //TODO: 考虑如何把该接口从Container 解耦
//    public Dispatcher getDispatcher() {
//        Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;
//        if(useThreadPool){
//            return new ThreadPoolDispatcher();
//        }else{
//            return new ThreadDispatcher();
//        }
//    }

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
        this.processors.putAll(processors);
    }

}
