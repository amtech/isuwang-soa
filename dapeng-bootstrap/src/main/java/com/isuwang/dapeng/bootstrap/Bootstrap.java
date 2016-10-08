package com.isuwang.dapeng.bootstrap;

import com.isuwang.dapeng.bootstrap.classloader.*;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Bootstrap
 *
 * @author craneding
 * @date 16/1/28
 */
public class Bootstrap {

    private static final List<URL> shareURLs = new ArrayList<>();
    private static final List<URL> platformURLs = new ArrayList<>();
    private static final List<List<URL>> appURLs = new ArrayList<>();
    private static final List<List<URL>> pluginURLs = new ArrayList<>();
    private static final String enginePath = System.getProperty("soa.base", new File(Bootstrap.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getParent());
    private static final String soaRunMode = System.getProperty("soa.run.mode", "maven");

    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        System.setProperty("soa.run.mode", soaRunMode);
        System.setProperty("soa.base", enginePath);

        System.out.println("soa.base:" + enginePath);
        System.out.println("soa.run.mode:" + soaRunMode);

        final boolean isRunInMaven = soaRunMode.equals("maven");

        if (!isRunInMaven) {
            loadAllUrls();

            loadAllClassLoader();
        }

        setAppClassLoader();

        startup();
    }

    private static void setAppClassLoader() {
        try {
            Class<?> springContainerClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.spring.SpringContainer");
            Field appClassLoaderField = springContainerClass.getField("appClassLoaders");
            appClassLoaderField.set(springContainerClass, ClassLoaderManager.appClassLoaders);

            Field pluginClassLoaderField = springContainerClass.getField("pluginClassLoaders");
            pluginClassLoaderField.set(springContainerClass, ClassLoaderManager.pluginClassLoaders);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static void startup() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Thread.currentThread().setContextClassLoader(ClassLoaderManager.platformClassLoader);
        Class<?> mainClass = ClassLoaderManager.platformClassLoader.loadClass("com.isuwang.dapeng.container.ContainerStartup");
        Method mainMethod = mainClass.getMethod("startup");
        mainMethod.invoke(mainClass);
    }

    private static void loadAllClassLoader() {
        ClassLoaderManager.shareClassLoader = new ShareClassLoader(shareURLs.toArray(new URL[shareURLs.size()]));

        ClassLoaderManager.platformClassLoader = new PlatformClassLoader(platformURLs.toArray(new URL[platformURLs.size()]));

        for (List<URL> appURL : appURLs) {
            AppClassLoader appClassLoader = new AppClassLoader(appURL.toArray(new URL[appURL.size()]));
            ClassLoaderManager.appClassLoaders.add(appClassLoader);
        }

        for (List<URL> pluginURL : pluginURLs) {
            PluginClassLoader pluginClassLoader = new PluginClassLoader(pluginURL.toArray(new URL[pluginURL.size()]));
            ClassLoaderManager.pluginClassLoaders.add(pluginClassLoader);
        }
    }

    private static void loadAllUrls() throws MalformedURLException {
        shareURLs.addAll(findJarURLs(new File(enginePath, "lib")));

        platformURLs.addAll(findJarURLs(new File(enginePath, "bin/lib")));

        final File appsPath = new File(enginePath, "apps");
        if (appsPath.exists() && appsPath.isDirectory()) {
            final File[] files = appsPath.listFiles();

            for (File file : files) {
                final List<URL> urlList = new ArrayList<>();

                if (file.isDirectory()) {
                    urlList.addAll(findJarURLs(file));
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    urlList.add(file.toURI().toURL());
                }
                if (!urlList.isEmpty())
                    appURLs.add(urlList);
            }
        }


        final File pluginPath = new File(enginePath, "plugin");
        if (pluginPath.exists() && pluginPath.isDirectory()) {
            final File[] files = pluginPath.listFiles();
            for (File file : files) {
                final List<URL> urlList = new ArrayList<>();
                if (file.isDirectory()) {
                    urlList.addAll(findJarURLs(file));
                } else if (file.isFile() && file.getName().endsWith(".jar")) {
                    urlList.add(file.toURI().toURL());
                }
                if (!urlList.isEmpty())
                    pluginURLs.add(urlList);
            }
        }

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
