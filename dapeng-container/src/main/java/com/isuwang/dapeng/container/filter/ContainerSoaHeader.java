package com.isuwang.dapeng.container.filter;

import com.isuwang.dapeng.core.IPUtils;
import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Container SoaHeader
 *
 * @author craneding
 * @date 16/3/15
 */
public class ContainerSoaHeader {

    private static HeaderProxy headerProxy = null;

    public static void setup() {
        if (headerProxy != null)
            return;

        InvocationContext.Factory.setSoaHeaderProxy(headerProxy = new HeaderProxy());
    }

    static class HeaderProxy implements InvocationContext.Factory.ISoaHeaderProxy {
        @Override
        public Optional<String> callerFrom() {
            return Optional.of("soaServer:" + IPUtils.localIp() + ":" + SoaSystemEnvProperties.SOA_CONTAINER_PORT);
        }

        @Override
        public Optional<Integer> customerId() {
            return Optional.empty();
        }

        @Override
        public Optional<String> customerName() {
            return Optional.empty();
        }

        @Override
        public Optional<Integer> operatorId() {
            return Optional.empty();
        }

        @Override
        public Optional<String> operatorName() {
            return Optional.empty();
        }

        @Override
        public Optional<String> sessionId() { return Optional.empty();}

        @Override
        public Map<String, String> attachments() {
            return new HashMap<>();
        }
    }

}
