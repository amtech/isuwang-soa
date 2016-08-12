package com.isuwang.dapeng.remoting.filter;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.registry.*;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tangliu on 2016/1/15.
 */
public class LoadBalanceFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadBalanceFilter.class);

    private static AtomicInteger roundRobinIndex = new AtomicInteger(8);

    @Override
    public void doFilter(FilterChain chain) throws TException {
        final InvocationContext context = (InvocationContext) chain.getAttribute(StubFilterChain.ATTR_KEY_CONTEXT);
        final SoaHeader soaHeader = (SoaHeader) chain.getAttribute(StubFilterChain.ATTR_KEY_HEADER);
        final boolean isLocal = SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local");

        String callerInfo = null;

        List<ServiceInfo> usableList;
        ServiceInfos serviceInfos;

        if (isLocal) {
            usableList = new ArrayList<>();
            serviceInfos = new ServiceInfos(false, usableList);
        } else {
            serviceInfos = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).loadMatchedServices(soaHeader.getServiceName(), soaHeader.getVersionName(), true);
            usableList = serviceInfos.getServiceInfoList();
        }
        chain.setAttribute(StubFilterChain.ATTR_KEY_USERING_FBZK, serviceInfos.isUsingFallbackZk());

        String serviceKey = soaHeader.getServiceName() + "." + soaHeader.getVersionName() + "." + soaHeader.getMethodName() + ".consumer";
        LoadBalanceStratage balance = getLoadBalanceStratage(serviceInfos.isUsingFallbackZk(), serviceKey) == null ? LoadBalanceStratage.LeastActive : getLoadBalanceStratage(serviceInfos.isUsingFallbackZk(), serviceKey);

        switch (balance) {
            case Random:
                callerInfo = random(callerInfo, usableList, chain);
                break;
            case RoundRobin:
                callerInfo = roundRobin(callerInfo, usableList, chain);
                break;
            case LeastActive:
                callerInfo = leastActive(callerInfo, usableList, chain);
                break;
            case ConsistentHash:
                break;
        }

        if (callerInfo != null) {
            LOGGER.info("{} {} {} target:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), callerInfo);

            String[] infos = callerInfo.split(":");
            context.setCalleeIp(infos[0]);
            context.setCalleePort(Integer.valueOf(infos[1]));
            context.getHeader().setVersionName(infos[2]);
        } else if (isLocal) {
            context.setCalleeIp(SoaSystemEnvProperties.SOA_SERVICE_IP);
            context.setCalleePort(SoaSystemEnvProperties.SOA_SERVICE_PORT);
        } else if (SoaSystemEnvProperties.SOA_SERVICE_IP_ISCONFIG) {
            context.setCalleeIp(SoaSystemEnvProperties.SOA_SERVICE_IP);
            context.setCalleePort(SoaSystemEnvProperties.SOA_SERVICE_PORT);
        }

        chain.doFilter();
    }


    public static String getCallerInfo(String serviceName, String versionName, String methodName) {

        final boolean isLocal = SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local");
        String callerInfo = null;

        List<ServiceInfo> usableList;
        ServiceInfos serviceInfos;
        if (isLocal) {
            usableList = new ArrayList<>();
            serviceInfos = new ServiceInfos(false, usableList);
        } else {
            serviceInfos = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).loadMatchedServices(serviceName, versionName, true);
            usableList = serviceInfos.getServiceInfoList();
        }

        String serviceKey = serviceName + "." + versionName + "." + methodName + ".consumer";
        LoadBalanceStratage balance = getLoadBalanceStratage(serviceInfos.isUsingFallbackZk(), serviceKey) == null ? LoadBalanceStratage.LeastActive : getLoadBalanceStratage(serviceInfos.isUsingFallbackZk(), serviceKey);

        switch (balance) {
            case Random:
                callerInfo = random(callerInfo, usableList, null);
                break;
            case RoundRobin:
                callerInfo = roundRobin(callerInfo, usableList, null);
                break;
            case LeastActive:
                callerInfo = leastActive(callerInfo, usableList, null);
                break;
            case ConsistentHash:
                break;
        }

        return callerInfo;
    }

    private static LoadBalanceStratage getLoadBalanceStratage(boolean usingFallbackZk, String key) {
        RegistryAgent currentInstance = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client);
        if (currentInstance == null)
            return null;

        Map<ConfigKey, Object> configs = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).getConfig(usingFallbackZk, key);
        if (null != configs) {
            return LoadBalanceStratage.findByValue((String) configs.get(ConfigKey.LoadBalance));
        }
        return null;
    }

    private static String random(String callerInfo, List<ServiceInfo> usableList, FilterChain chain) {
        //随机选择一个可用server
        if (usableList.size() > 0) {
            final Random random = new Random();

            final int index = random.nextInt(usableList.size());

            ServiceInfo serviceInfo = usableList.get(index);
            if (chain != null)
                chain.setAttribute(StubFilterChain.ATTR_KEY_SERVERINFO, serviceInfo);
            LOGGER.info(serviceInfo.getHost() + ":" + serviceInfo.getPort() + " concurrency：" + serviceInfo.getActiveCount());

            callerInfo = serviceInfo.getHost() + ":" + String.valueOf(serviceInfo.getPort()) + ":" + serviceInfo.getVersionName();
        }
        return callerInfo;
    }

    private static String leastActive(String callerInfo, List<ServiceInfo> usableList, FilterChain chain) {

        if (usableList.size() > 0) {

            AtomicInteger count = usableList.get(0).getActiveCount();
            int index = 0;

            for (int i = 1; i < usableList.size(); i++) {
                if (usableList.get(i).getActiveCount().intValue() < count.intValue()) {
                    index = i;
                }
            }

            ServiceInfo serviceInfo = usableList.get(index);
            if (chain != null)
                chain.setAttribute(StubFilterChain.ATTR_KEY_SERVERINFO, serviceInfo);

            callerInfo = serviceInfo.getHost() + ":" + String.valueOf(serviceInfo.getPort()) + ":" + serviceInfo.getVersionName();
        }
        return callerInfo;
    }

    private static String roundRobin(String callerInfo, List<ServiceInfo> usableList, FilterChain chain) {

        if (usableList.size() > 0) {
            roundRobinIndex = new AtomicInteger(roundRobinIndex.incrementAndGet() % usableList.size());
            ServiceInfo serviceInfo = usableList.get(roundRobinIndex.get());

            if (chain != null)
                chain.setAttribute(StubFilterChain.ATTR_KEY_SERVERINFO, serviceInfo);

            callerInfo = serviceInfo.getHost() + ":" + String.valueOf(serviceInfo.getPort()) + ":" + serviceInfo.getVersionName();
        }
        return callerInfo;
    }

}
