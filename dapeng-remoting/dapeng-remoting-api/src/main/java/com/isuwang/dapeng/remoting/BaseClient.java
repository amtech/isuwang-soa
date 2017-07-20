package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.dapeng.registry.RegistryAgent;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.dapeng.registry.conf.SoaRegistry;
import com.isuwang.dapeng.remoting.conf.SoaRemoting;
import com.isuwang.dapeng.remoting.conf.SoaRemotingConnectionPool;
import com.isuwang.dapeng.remoting.conf.SoaRemotingFilter;
import com.isuwang.dapeng.remoting.conf.SoaRemotingFilters;
import com.isuwang.dapeng.remoting.filter.StubFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created by tangliu on 17/7/20.
 */
public class BaseClient {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseClient.class);

    public static SoaConnectionPool connectionPool;

    static {

        final ClassLoader classLoader = BaseClient.class.getClassLoader();

        try (InputStream is = getInputStream("dapeng-remoting-conf.xml")) {

            final SoaRemoting soaRemoting = JAXB.unmarshal(is, SoaRemoting.class);
            final SoaRemotingFilters remotingFilters = soaRemoting.getSoaRemotingFilters();

            // load filter
            for (SoaRemotingFilter remotingFilter : remotingFilters.getSoaRemotingFilter()) {
                Class<?> aClass = classLoader.loadClass(remotingFilter.getRef());

                StubFilterChain.addFilter((Filter) aClass.newInstance());

                LOGGER.info("client load filter {} with path {}", remotingFilter.getName(), remotingFilter.getRef());
            }

            // load connection pool
            final SoaRemotingConnectionPool pool = soaRemoting.getSoaRemotingConnectionPool();
            final Class<?> aClass = classLoader.loadClass(pool.getRef());
            BaseClient.connectionPool = (SoaConnectionPool) aClass.newInstance();


        } catch (Exception e) {
            LOGGER.error("client load filter error", e);
        }

        if (!SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            try (InputStream is = getInputStream("dapeng-registry-conf.xml")) {
                final SoaRegistry soaRegistry = JAXB.unmarshal(is, SoaRegistry.class);

                Class<?> aClass = classLoader.loadClass(soaRegistry.getRef());

                RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Client, (RegistryAgent) aClass.newInstance());
                RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).start();
                LOGGER.info("client load registry {} with path {}", soaRegistry.getName(), soaRegistry.getRef());
            } catch (Exception e) {
                LOGGER.error("client load registry error", e);
            }
        } else {
            LOGGER.info("soa remoting mode is {},client not load registry", SoaSystemEnvProperties.SOA_REMOTING_MODE);
        }

    }

    static InputStream getInputStream(String name) throws FileNotFoundException {
        InputStream stream = BaseServiceClient.class.getClassLoader().getResourceAsStream(name);

        if (stream == null)
            return BaseServiceClient.class.getResourceAsStream(name);

        if (stream == null)
            throw new FileNotFoundException("not found " + name);

        return stream;
    }
}
