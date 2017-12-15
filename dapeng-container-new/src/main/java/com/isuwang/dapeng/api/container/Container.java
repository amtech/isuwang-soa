package com.isuwang.dapeng.api.container;

import com.isuwang.dapeng.api.extension.Dispatcher;
import com.isuwang.dapeng.api.listeners.AppListener;
import com.isuwang.dapeng.api.plugins.Plugin;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.impl.filters.SharedChain;
import com.isuwang.dapeng.impl.handler.SoaServiceDefinition;

import java.util.List;
import java.util.Map;

/**
 * 大鹏容器的主结构，负责管理容器相关的监听器，插件，应用程序。
 *
 * 所有的组件的注册，卸载动作都应该由Container来负责，
 */
public interface Container {

    /**
     * 注册应用程序监听器，
     * @param listener
     */
    public void registerAppListener(AppListener listener);

    /**
     * 卸载用用程序监听器
     * @param listener
     */
    public void unregisterAppListener(AppListener listener);

    /**
     * 注册应用程序（保存容器具体的应用信息）
     * @param app
     */
    public void registerApplication(Application app);

    /**
     * 卸载应用程序
     * @param app
     */
    public void unregisterApplication(Application app);

    /**
     * 注册插件(like: Zookeeper,netty..etc.)
     * @param plugin
     */
    public void registerPlugin(Plugin plugin);

    /**
     * 卸载插件
     * @param plugin
     */
    public void unregisterPlugin(Plugin plugin);

    /**
     * 获取应用程序的相关信息
     * @return
     */
    public List<Application> getApplications();

    public Dispatcher getDispatcher();

    public SharedChain getSharedChain();

    public List<Plugin> getPlugins();

    Map<ProcessorKey, SoaServiceDefinition<?>> getServiceProcessors();

    void registerAppProcessors(Map<ProcessorKey, SoaServiceDefinition<?>> processors);



}
