package com.isuwang.dapeng.bootstrap.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * App Class Loader
 *
 * @author tangliu
 * @date 16/9/18
 */
public class PluginClassLoader extends URLClassLoader {

    public PluginClassLoader(URL[] urls) {
        super(urls, ClassLoader.getSystemClassLoader());
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (name.startsWith("com.isuwang.dapeng.core")
                || name.startsWith("com.isuwang.org.apache.thrift")
                || name.startsWith("com.isuwang.dapeng.transaction.api")
                || name.startsWith("com.google.gson")
                || name.startsWith("org.slf4j"))
            return ClassLoaderManager.shareClassLoader.loadClass(name);
        return super.loadClass(name, resolve);
    }
}
