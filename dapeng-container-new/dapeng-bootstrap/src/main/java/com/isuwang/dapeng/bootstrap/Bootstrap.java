package com.isuwang.dapeng.bootstrap;

import com.isuwang.dapeng.bootstrap.classloader.ApplicationClassLoader;
import com.isuwang.dapeng.bootstrap.classloader.ContainerClassLoader;
import com.isuwang.dapeng.bootstrap.classloader.CoreClassLoader;
import com.isuwang.dapeng.bootstrap.classloader.PluginClassLoader;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Bootstrap {
    private static final String enginePath = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParentFile().getParent() + "/dapeng-container-impl/target/dapeng-container/");


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        List<URL> coreURLs;
        List<URL> containerURLs;
        List<List<URL>> applicationURLs = new ArrayList<>();
        List<List<URL>> pluginURLs = new ArrayList<>();

        // 支持maven或者sbt的方式传入application路径
        if (args != null && args.length > 0) {
            List<URL> urls = new ArrayList<>();
            for (String arg: args) {
                URL url = new URL("file:" + arg);
                urls.add(url);
            }
            applicationURLs.add(urls);
            containerURLs = new ArrayList<>();
            containerURLs.addAll(urls);
            coreURLs = new ArrayList<>();
            coreURLs.addAll(urls);
        } else {
            coreURLs = findJarURLs(new File(enginePath, "lib"));
            containerURLs = findJarURLs(new File(enginePath, "bin/lib"));
            applicationURLs.addAll(getUrlList(new File(enginePath, "apps")));
            pluginURLs.addAll(getUrlList(new File(enginePath, "plugin")));
        }

        CoreClassLoader coreClassLoader = new CoreClassLoader(coreURLs.toArray(new URL[coreURLs.size()]));

        ClassLoader platformClassLoader = new ContainerClassLoader(containerURLs.toArray(new URL[containerURLs.size()]), coreClassLoader);

        List<ClassLoader> applicationCls = applicationURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]), coreClassLoader)).collect(Collectors.toList());

        startup(platformClassLoader, applicationCls);
    }


    private static void startup(ClassLoader platformClassLoader, List<ClassLoader> applicationCls) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Thread.currentThread().setContextClassLoader(platformClassLoader);
        Class<?> containerFactoryClz = platformClassLoader.loadClass("com.isuwang.dapeng.api.ContainerFactory");
        Method createContainerMethod = containerFactoryClz.getMethod("createContainer", List.class, ClassLoader.class);
        createContainerMethod.invoke(containerFactoryClz, applicationCls, platformClassLoader);

        Method getContainerMethod = containerFactoryClz.getMethod("getContainer");
        Object container = getContainerMethod.invoke(containerFactoryClz);

        Method mainMethod = container.getClass().getMethod("startup");
        mainMethod.invoke(container);
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
