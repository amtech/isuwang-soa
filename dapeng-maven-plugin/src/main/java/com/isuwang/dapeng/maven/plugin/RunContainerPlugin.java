package com.isuwang.dapeng.maven.plugin;

import com.isuwang.dapeng.bootstrap.Bootstrap;
import com.isuwang.dapeng.bootstrap.classloader.*;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Run Container Plugin
 *
 * @author craneding
 * @date 16/1/25
 */
@Mojo(name = "run", threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class RunContainerPlugin extends SoaAbstractMojo {

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (project == null)
            throw new MojoExecutionException("not found project.");

        getLog().info("bundle:" + project.getGroupId() + ":" + project.getArtifactId() + ":" + project.getVersion());

        System.setProperty("soa.base", new File(project.getBuild().getOutputDirectory()).getAbsolutePath().replace("/target/classes", ""));
        System.setProperty("soa.run.mode", "maven");

        final String mainClass = Bootstrap.class.getName();

        IsolatedThreadGroup threadGroup = new IsolatedThreadGroup(mainClass);
        Thread bootstrapThread = new Thread(threadGroup, () -> {
            try {
                //ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

                URL[] urls = ((URLClassLoader) Thread.currentThread().getContextClassLoader()).getURLs();

                List<URL> shareUrls = new ArrayList<>(Arrays.asList(urls));
                Iterator<URL> iterator = shareUrls.iterator();
                while (iterator.hasNext()) {
                    URL url = iterator.next();

                    if (url.getFile().matches("^.*/dapeng-transaction-impl.*\\.jar$")) {
                        iterator.remove();

                        continue;
                    }

                    if (removeContainerAndBootstrap(iterator, url)) continue;

                    if (removeServiceProjectArtifact(iterator, url)) continue;
                }
//                List<URL> pluginUrls = new ArrayList<>(Arrays.asList(urls));
//                iterator = pluginUrls.iterator();
//                while (iterator.hasNext()) {
//                    URL url = iterator.next();
//                    if (url.getFile().matches("^.*/dapeng-transaction-impl.*\\.jar$")) {
//                        iterator.remove();
//                        continue;
//                    }
//                    if (removeContainerAndBootstrap(iterator, url)) continue;
//                    if (removeServiceProjectArtifact(iterator, url)) continue;
//                }

                List<URL> appUrls = new ArrayList<>(Arrays.asList(urls));
                iterator = appUrls.iterator();
                while (iterator.hasNext()) {
                    URL url = iterator.next();
                    if (removeTwitterAndScalaDependency(iterator,url)) continue;
                    if (removeContainerAndBootstrap(iterator, url)) continue;
                }

                List<URL> platformUrls = new ArrayList<>(Arrays.asList(urls));
                iterator = platformUrls.iterator();
                while (iterator.hasNext()) {
                    URL url = iterator.next();
                    if (removeServiceProjectArtifact(iterator, url)) continue;
                    if (removeTwitterAndScalaDependency(iterator,url)) continue;
                }

                ClassLoaderManager.shareClassLoader = new ShareClassLoader(shareUrls.toArray(new URL[shareUrls.size()]));
                ClassLoaderManager.platformClassLoader = new PlatformClassLoader(platformUrls.toArray(new URL[platformUrls.size()]));
                ClassLoaderManager.appClassLoaders.add(new AppClassLoader(appUrls.toArray(new URL[appUrls.size()])));
                ClassLoaderManager.pluginClassLoaders.add(new PluginClassLoader(shareUrls.toArray(new URL[shareUrls.size()])));

                Bootstrap.main(new String[]{});
            } catch (Exception e) {
                Thread.currentThread().getThreadGroup().uncaughtException(Thread.currentThread(), e);
            }
        }, mainClass + ".main()");
        bootstrapThread.setContextClassLoader(getClassLoader());
        bootstrapThread.start();

        joinNonDaemonThreads(threadGroup);
    }

    private boolean removeContainerAndBootstrap(Iterator<URL> iterator, URL url) {
        if (url.getFile().matches("^.*/dapeng-container.*\\.jar$")) {
            iterator.remove();

            return true;
        }

        if (url.getFile().matches("^.*/dapeng-bootstrap.*\\.jar$")) {
            iterator.remove();

            return true;
        }
        return false;
    }

    private boolean removeTwitterAndScalaDependency(Iterator<URL> iterator, URL url){
        if(url.getFile().matches("^.*/twitter.*\\.jar$")) {
            iterator.remove();
            return true;
        }
        if(url.getFile().matches("^.*/scala.*\\.jar$")) {
            iterator.remove();
            return true;
        }
        return false;
    }

    private boolean removeServiceProjectArtifact(Iterator<URL> iterator, URL url) {
        String regex = project.getArtifact().getFile().getAbsolutePath().replaceAll("\\\\", "/");

        if (File.separator.equals("\\"))
            regex = regex.replace(File.separator, File.separator + File.separator);

        if (url.getFile().matches("^.*" + regex + ".*$")) {
            iterator.remove();

            return true;
        }
        return false;
    }

}
