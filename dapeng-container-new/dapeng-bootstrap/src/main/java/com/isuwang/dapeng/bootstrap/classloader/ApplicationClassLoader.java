package com.isuwang.dapeng.bootstrap.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * App Class Loader
 *
 * @author craneding
 * @date 16/1/28
 */
public class ApplicationClassLoader extends URLClassLoader {

    private final ClassLoader coreClassLoader;

    public ApplicationClassLoader(URL[] urls, ClassLoader coreClassLoader) {
        super(urls, ClassLoader.getSystemClassLoader());
        this.coreClassLoader = coreClassLoader;
    }

    public ApplicationClassLoader(URL[] urls, ClassLoader parent, ClassLoader coreClassLoader) {
        super(urls, parent);
        this.coreClassLoader = coreClassLoader;
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {

        if (name.startsWith("com.isuwang.dapeng.core")
                || name.startsWith("com.isuwang.org.apache.thrift")
                || name.startsWith("com.isuwang.dapeng.transaction.api")
                || name.startsWith("com.google.gson")) {
            return coreClassLoader.loadClass(name);
        }
        return super.loadClass(name, resolve);
    }
}
