package com.isuwang.dapeng.impl;


import com.isuwang.dapeng.api.ContainerFactory;
import com.isuwang.dapeng.api.Plugin;
import com.isuwang.dapeng.impl.classloader.*;
import com.isuwang.dapeng.impl.container.DapengContainer;
import com.isuwang.dapeng.impl.plugins.*;
import com.isuwang.dapeng.impl.plugins.netty.NettyPlugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Bootstrap {

    private static final List<URL> shareURLs = new ArrayList<>();
    private static final List<URL> platformURLs = new ArrayList<>();
    private static final List<List<URL>> appURLs = new ArrayList<>();
    private static final List<List<URL>> pluginURLs = new ArrayList<>();
    private static final String enginePath = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParentFile().getParent() +"/dapeng-container/");


    public static void main(String[] args) throws MalformedURLException {

        //1. 初始化dapeng容器
        DapengContainer dapengContainer = new DapengContainer();
        ContainerFactory.initDapengContainer(dapengContainer);

        System.out.println("enginePath:" + enginePath);


        loadAllUrls();
        List<AppClassLoader> appClassLoaders = appURLs.stream().map(i -> new AppClassLoader(i.toArray(new URL[i.size()]))).collect(Collectors.toList());

        PlatformClassLoader platformClassLoader = new PlatformClassLoader(platformURLs.toArray(new URL[platformURLs.size()]));
        ClassLoaderManager.platformClassLoader = platformClassLoader;
        ShareClassLoader shareClassLoader = new ShareClassLoader(shareURLs.toArray(new URL[shareURLs.size()]));
        ClassLoaderManager.shareClassLoader = shareClassLoader;
        List<PluginClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]))).collect(Collectors.toList());
        ClassLoaderManager.pluginClassLoader = pluginClassLoaders;

        //3. 初始化appLoader,dapengPlugin
        Plugin springAppLoader = new SpringAppLoader(dapengContainer,appClassLoaders);
        Plugin apiDocPlugin = new ApiDocPlugin(dapengContainer);
        Plugin zookeeperPlugin = new ZookeeperRegistryPlugin(dapengContainer);
        Plugin taskSchedulePlugin = new TaskSchedulePlugin(dapengContainer);
        Plugin nettyPlugin = new NettyPlugin(dapengContainer);

        //ApiDocPlugin优先启动(为了Spring触发注册事件时，ServiceCache已经实例化，能收到消息)
        dapengContainer.registerPlugin(springAppLoader);
        dapengContainer.registerPlugin(zookeeperPlugin);
        dapengContainer.registerPlugin(taskSchedulePlugin);
        dapengContainer.registerPlugin(nettyPlugin);
        dapengContainer.registerPlugin(apiDocPlugin);


        //4.启动Apploader， plugins
        //ContainerFactory.getContainer().getPlugins().forEach(Plugin::start);
        springAppLoader.start();
        nettyPlugin.start();
        apiDocPlugin.start();

//        PluginLoader pluginLoader = new PluginLoader();
//        pluginLoader.startup();


    }


    private static void loadAllUrls() throws MalformedURLException {
        shareURLs.addAll(findJarURLs(new File(enginePath, "lib")));

        platformURLs.addAll(findJarURLs(new File(enginePath, "bin/lib")));

        final File appsPath = new File(enginePath, "apps");
        getUrlList(appsPath).stream().forEach(i -> appURLs.add(i));

        final File pluginPath = new File(enginePath, "plugin");
        getUrlList(pluginPath).stream().forEach(i -> pluginURLs.add(i));
    }


    private static List<List<URL>> getUrlList(File filepath) throws MalformedURLException {
        List<List<URL>> urlsList = new ArrayList<>();
        if (filepath.exists() && filepath.isDirectory()) {
            final File[] files = filepath.listFiles();
            for (File file : files) {
                final List<URL> urlList = new ArrayList<>();
                if (file.isDirectory()) {
                    urlList.addAll(findJarURLs(file));
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    urlList.add(file.toURI().toURL());
                }
                if (!urlList.isEmpty()) {
                    urlsList.add(urlList);
                }
            }
        }
        return urlsList;
    }

    private static List<URL> findJarURLs(File file) throws MalformedURLException {
        final List<URL> urlList = new ArrayList<>();

        if (file != null && file.exists()) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                urlList.add(file.toURI().toURL());
            } else if (file.isDirectory()) {
                File[] files = file.listFiles();
                if (files != null) {
                    for (int i = 0; i < files.length; i++) {
                        urlList.addAll(findJarURLs(files[i]));
                    }
                }
            }
        }

        return urlList;
    }
}
