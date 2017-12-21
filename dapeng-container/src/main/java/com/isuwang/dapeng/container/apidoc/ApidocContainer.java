package com.isuwang.dapeng.container.apidoc;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.doc.ApiWebSite;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apidoc Container
 *
 * @author craneding
 * @date 16/3/7
 */
public class ApidocContainer implements Container {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApidocContainer.class);

    Server server = null;

    @Override
    public void start() {
//        Thread thread = new Thread("api-doc-thread") {
//            @Override
//            public void run() {
//                try {
//                    server = ApiWebSite.createServer(SoaSystemEnvProperties.SOA_APIDOC_PORT);
//
//                    server.start();
//                    System.out.println("api-doc server started at port: " + SoaSystemEnvProperties.SOA_APIDOC_PORT);
//
//                    server.join();
//                } catch (Exception e) {
//                    LOGGER.error(e.getMessage(), e);
//                }
//            }
//        };
//        thread.setContextClassLoader(ApidocContainer.class.getClassLoader());
//        thread.start();
    }

    @Override
    public void stop() {
        if (server != null)
            try {
                server.stop();
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
    }

}
