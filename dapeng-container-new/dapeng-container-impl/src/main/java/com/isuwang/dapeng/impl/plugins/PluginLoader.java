package com.isuwang.dapeng.impl.plugins;

import com.isuwang.dapeng.api.Plugin;

import java.util.ServiceLoader;

public class PluginLoader {

    public void startup() {
        ServiceLoader<Plugin> plugins = ServiceLoader.load(Plugin.class, PluginLoader.class.getClassLoader());

        for (Plugin plugin: plugins) {
            System.out.println(" what the????");
            plugin.start();
        }

    }
}
