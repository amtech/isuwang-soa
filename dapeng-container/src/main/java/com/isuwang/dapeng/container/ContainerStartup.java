package com.isuwang.dapeng.container;

import com.isuwang.dapeng.container.apidoc.ApidocContainer;
import com.isuwang.dapeng.container.filter.ContainerFilter;
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

import static java.util.stream.Collectors.toList;

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
    public static List<Filter> filters = new ArrayList<>();

    public static void startup() {
        final long startTime = System.currentTimeMillis();

        final List<Container> containers = new ArrayList<>();
        ServiceLoader<ContainerFilter> filterServiceLoader = ServiceLoader.load(ContainerFilter.class,ContainerStartup.class.getClassLoader());
        ServiceLoader<Container> containerServiceLoader = ServiceLoader.load(Container.class,ContainerStartup.class.getClassLoader());


        // 本地模式
        final boolean localMode = SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local");

        // 剔除Registry的容器
        for (Container container : containerServiceLoader) {
            if (localMode && (container.getClass().getName().contains("ZookeeperRegistryContainer"))) {
                continue;
            }
            if (!localMode && (container.getClass().getName().contains("LocalRegistryContainer"))) {
                continue;
            }
            containers.add(container);
            System.out.println("load container " + container.getClass().getName());
        }

        if ("maven".equals(SOA_RUN_MODE)) {
            containers.add(new ApidocContainer());
        }

        for (ContainerFilter filter : filterServiceLoader) {
            filters.add(filter);
        }

        if (!SoaSystemEnvProperties.SOA_MONITOR_ENABLE) {
            List<Filter> collect = filters
                    .stream()
                    .filter(filter -> filter.getClass().getName().equals("com.isuwang.dapeng.container.filter.QPSStatFilter") || filter.getClass().getName().equals("com.isuwang.dapeng.container.filter.PlatformProcessDataFilter"))
                    .collect(toList());

            if (collect.size() > 0)
                filters.removeAll(collect);
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
