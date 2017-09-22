package com.isuwang.dapeng.container;

import com.isuwang.dapeng.container.apidoc.ApidocContainer;
import com.isuwang.dapeng.container.conf.SoaServer;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * ContainerStartup
 *
 * @author craneding
 * @date 16/1/18
 */
public class ContainerStartup {

    private static volatile boolean running = true;
    public static final String SOA_BASE = System.getProperty("soa.base");
    public static final String SOA_RUN_MODE = System.getProperty("soa.run.mode", "maven");
    public static SoaServer soaServer = null;
    public static List<Filter> filters = new ArrayList<>();

    public static void startup() {
        final long startTime = System.currentTimeMillis();

        final List<Container> containers = new ArrayList<>();
        ServiceLoader<Filter> filterServiceLoader = ServiceLoader.load(Filter.class);
        ServiceLoader<Container> containerServiceLoader = ServiceLoader.load(Container.class);

        for (Filter filter : filterServiceLoader) {
            filters.add(filter);
        }

        // 本地模式
        final boolean localMode = SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local");

        for (Container container : containerServiceLoader) {
            if (localMode && (container.getClass().getName().equals("com.isuwang.dapeng.container.registry.ZookeeperRegistryContainer"))) {
                continue;
            }
            containers.add(container);
            System.out.println("load container " + container.getClass().getName());
        }

        if ("maven".equals(SOA_RUN_MODE)) {
            containers.add(new ApidocContainer());
        }

        try {
            containers.forEach(Container::start);
        } catch (Throwable e) {
            e.printStackTrace();

            System.exit(-1);
        }

        final Logger logger = LoggerFactory.getLogger(ContainerStartup.class);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                for (Container container : containers)
                    container.stop();

                synchronized (ContainerStartup.class) {
                    running = false;

                    ContainerStartup.class.notify();
                }
            }
        });

        logger.info("Server startup in {} ms", System.currentTimeMillis() - startTime);

        synchronized (ContainerStartup.class) {
            while (running) {
                try {
                    ContainerStartup.class.wait();
                } catch (InterruptedException e) {
                }

                logger.info("Server shutdown");
            }
        }
    }

    public static InputStream loadInputStreamInClassLoader(String path) throws FileNotFoundException {
        if (SOA_RUN_MODE.endsWith("maven"))
            return ContainerStartup.class.getClassLoader().getResourceAsStream(path);
        return new FileInputStream(new File(SOA_BASE, "conf/" + path));
    }

}
