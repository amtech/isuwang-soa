package com.isuwang.dapeng.container.transaction;

import com.isuwang.dapeng.container.Container;
import com.isuwang.dapeng.container.spring.SpringContainer;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.transaction.api.GlobalTransactionFactory;
import com.isuwang.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.isuwang.dapeng.transaction.api.service.GlobalTransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * TransactionContainer
 *
 * @author craneding
 * @date 16/4/11
 */
public class TransactionContainer implements Container {
    private static final Logger LOGGER = LoggerFactory.getLogger(SpringContainer.class);

    private ClassPathXmlApplicationContext context;

    @Override
    public void start() {
        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE) {
            String configPath = System.getProperty(SpringContainer.SPRING_CONFIG);
            if (configPath == null || configPath.length() <= 0) {
                configPath = SpringContainer.DEFAULT_SPRING_CONFIG;
            }

            try {
                List<String> xmlPaths = new ArrayList<>();

                Enumeration<URL> resources = TransactionContainer.class.getClassLoader().getResources(configPath);

                while (resources.hasMoreElements()) {
                    URL nextElement = resources.nextElement();

                    if (nextElement.toString().matches(".*dapeng-transaction.*"))
                        xmlPaths.add(nextElement.toString());
                }

                context = new ClassPathXmlApplicationContext(xmlPaths.toArray(new String[0]));
                context.start();

                GlobalTransactionFactory.setGlobalTransactionService(context.getBeansOfType(GlobalTransactionService.class).values().iterator().next());
                GlobalTransactionFactory.setGlobalTransactionProcessService(context.getBeansOfType(GlobalTransactionProcessService.class).values().iterator().next());

//                if (!MasterHelper.isMaster("com.isuwang.dapeng.transaction.api.service.GlobalTransactionService", "1.0.0")) {
//                    RegistryAgent agent = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server);
//                    agent.registerService("com.isuwang.dapeng.transaction.api.service.GlobalTransactionService", "1.0.0");
//                }

            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void stop() {
        if (context != null)
            context.stop();
    }

}
