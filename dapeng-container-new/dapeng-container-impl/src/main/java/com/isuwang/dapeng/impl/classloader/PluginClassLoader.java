package com.isuwang.dapeng.impl.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * App Class Loader
 *
 * @author tangliu
 * @date 16/9/18
 */
public class PluginClassLoader extends URLClassLoader {

    private final CoreClassLoader coreClassLoader;

    public PluginClassLoader(URL[] urls, CoreClassLoader coreClassLoader) {
        super(urls, ClassLoader.getSystemClassLoader());
        this.coreClassLoader = coreClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (name.startsWith("com.isuwang.dapeng.core")
                || name.startsWith("com.isuwang.org.apache.thrift")
                || name.startsWith("com.isuwang.dapeng.transaction.api")
                || name.startsWith("com.google.gson")
                || name.startsWith("org.slf4j"))
            return coreClassLoader.loadClass(name);
        return super.loadClass(name, resolve);
    }
}
