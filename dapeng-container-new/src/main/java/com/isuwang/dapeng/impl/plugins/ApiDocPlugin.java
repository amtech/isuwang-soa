package com.isuwang.dapeng.impl.plugins;

import com.isuwang.dapeng.core.AppListener;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.api.Container;
import com.isuwang.dapeng.api.Plugin;
import com.isuwang.dapeng.core.events.AppEvent;
import com.isuwang.dapeng.doc.ApiWebSite;
import com.isuwang.dapeng.doc.TestController;
import com.isuwang.dapeng.doc.cache.ServiceCache;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.Controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ApiDocPlugin implements AppListener, Plugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiDocPlugin.class);
    private Container container;
    private Server server = null;
    private static final ServiceCache serviceCache = new ServiceCache();

    List<Controller> controllers = new ArrayList<>();

    public ApiDocPlugin(Container container) {
        this.container = container;
        this.container.registerAppListener(this);
        this.container.registerAppListener(serviceCache);

    }

    @Override
    public void appRegistered(AppEvent event) {
        //TODO:
        LOGGER.info(" ApiDocPlugin received appRegistered event.....");
    }

    @Override
    public void appUnRegistered(AppEvent event) {
        LOGGER.info(" ApiDocPlugin received appUnregistered event.....");
    }

    @Override
    public void start() {
        Thread thread = new Thread("api-doc-thread") {
            @Override
            public void run() {
                try {

                    List<Object> extensionBeans = new ArrayList<>();
                    extensionBeans.add(serviceCache);

                    server = ApiWebSite.createServer(SoaSystemEnvProperties.SOA_APIDOC_PORT, extensionBeans);

                    server.start();
                    System.out.println("api-doc server started at port: " + SoaSystemEnvProperties.SOA_APIDOC_PORT);

                    server.join();
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                }
            }
        };
        thread.setContextClassLoader(ApiDocPlugin.class.getClassLoader());
        thread.start();
    }

    @Override
    public void stop() {

    }
}
