package com.isuwang.dapeng.bootstrap.classloader;

import java.util.ArrayList;
import java.util.List;

/**
 * Class Loader Manager
 *
 * @author craneding
 * @date 16/1/28
 */
public class ClassLoaderManager {

    public static ClassLoader shareClassLoader;

    public static ClassLoader platformClassLoader;

    public static List<ClassLoader> appClassLoaders = new ArrayList<>();

    public static List<ClassLoader> pluginClassLoaders = new ArrayList<>();
}
