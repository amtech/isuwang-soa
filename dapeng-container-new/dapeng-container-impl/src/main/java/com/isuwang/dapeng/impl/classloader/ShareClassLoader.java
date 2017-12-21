package com.isuwang.dapeng.impl.classloader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Share Class Loader
 *
 * @author craneding
 * @date 16/1/28
 */
public class ShareClassLoader extends URLClassLoader {

    public ShareClassLoader(URL[] urls) {
        super(urls, Thread.currentThread().getContextClassLoader());
    }

}
