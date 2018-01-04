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

    private static final List<URL> shareURLs = new ArrayList<>();
    private static final List<URL> platformURLs = new ArrayList<>();
    private static final List<List<URL>> appURLs = new ArrayList<>();
    private static final List<List<URL>> pluginURLs = new ArrayList<>();
    private static final String enginePath = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().getParentFile().getParent() +"/dapeng-container/");


    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        if (args != null && args.length > 0) {
            List<URL> urls = new ArrayList<>();
            for (String arg: args) {
                URL url = new URL("file:" + arg);
                urls.add(url);
            }
            appURLs.add(urls);
            platformURLs.addAll(urls);
            shareURLs.addAll(urls);
        } else {
            loadAllUrls();
        }

        System.out.println(" appURL: " + appURLs.size() + " platformUrls: " + platformURLs.size() + " shareUrl: " + shareURLs.size());

        CoreClassLoader coreClassLoader = new CoreClassLoader(shareURLs.toArray(new URL[shareURLs.size()]));

        ClassLoader platformClassLoader = new ContainerClassLoader(platformURLs.toArray(new URL[platformURLs.size()]),coreClassLoader);

        List<ClassLoader> applicationCls = appURLs.stream().map(i -> new ApplicationClassLoader(i.toArray(new URL[i.size()]),coreClassLoader)).collect(Collectors.toList());

        List<ClassLoader> pluginClassLoaders = pluginURLs.stream().map(i -> new PluginClassLoader(i.toArray(new URL[i.size()]),coreClassLoader)).collect(Collectors.toList());

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
