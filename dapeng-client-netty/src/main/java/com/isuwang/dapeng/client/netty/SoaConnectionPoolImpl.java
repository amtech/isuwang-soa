package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.registry.*;
import com.isuwang.dapeng.registry.zookeeper.LoadBalanceService;
import com.isuwang.dapeng.registry.zookeeper.ZkClientAgentImpl;
import com.isuwang.dapeng.util.SoaSystemEnvProperties;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private Map<String, ServiceZKInfo> zkInfos = new ConcurrentHashMap<>();
    private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();
    private ZkClientAgent zkAgent = new ZkClientAgentImpl();

    public SoaConnectionPoolImpl() {
        IdleConnectionManager connectionManager = new IdleConnectionManager();
        connectionManager.start();
    }

    //TODO
    List<WeakReference<ClientInfo>> clientInfos;

    // TODO connection idle process.
    Thread cleanThread = null;  // clean idle connections;
    // TODO ClientInfo clean.


    private boolean checkVersion(String reqVersion, String targetVersion) {
        // x.y.z
        // x.Y.Z Y.Z >= y.z
        return true;
    }

    @Override
    public ClientInfo registerClientInfo(String serviceName, String version) {
        // TODO
        // clientInfos.add(new WeakReference<ClientInfo>(client));

        zkAgent.syncService(serviceName, zkInfos);
        return null;
    }

    @Override
    public <REQ, RESP> RESP send(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws SoaException {

        SoaConnection connection = findConnection(service, version, method);

        if (connection == null) {
            throw new SoaException(SoaCode.NotConnected);
        }

        return connection.send(service, version, method, request, requestSerializer, responseSerializer);
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer, long timeout) throws SoaException {

        SoaConnection connection = findConnection(service, version, method);

        if (connection == null) {
            throw new SoaException(SoaCode.NotConnected);
        }

        return connection.sendAsync(service, version, method, request, requestSerializer, responseSerializer, timeout);
    }

    public SoaConnection findConnection(String service, String version, String method) {
        ServiceZKInfo zkInfo = zkInfos.get(service);

        List<RuntimeInstance> compatibles = zkInfo.getRuntimeInstances().stream().filter(rt -> {
            return checkVersion(version, rt.version);
        }).collect(Collectors.toList());

        String serviceKey = service + "." + version + "." + method + ".consumer";
        RuntimeInstance inst = loadbalance(serviceKey, compatibles);

        inst.getActiveCount().incrementAndGet();

        IpPort ipPort = new IpPort(inst.ip, inst.port);
        SubPool subPool = subPools.get(ipPort);
        if (subPool == null) {
            subPool = new SubPool(inst.ip, inst.port);
            subPools.put(ipPort, subPool);
        }

        return subPool.getConnection();
    }

    private RuntimeInstance loadbalance(String serviceKey, List<RuntimeInstance> compatibles) {

        boolean usingFallbackZookeeper = SoaSystemEnvProperties.SOA_ZOOKEEPER_FALLBACK_ISCONFIG;
        LoadBalanceStratage balance = LoadBalanceStratage.Random;

        Map<ConfigKey, Object> configs = zkAgent.getConfig(usingFallbackZookeeper, serviceKey);
        if (null != configs) {
            balance = LoadBalanceStratage.findByValue((String) configs.get(ConfigKey.LoadBalance));
        }

        RuntimeInstance instance = null;
        switch (balance) {
            case Random:
                instance = LoadBalanceService.random(compatibles);
                break;
            case RoundRobin:
                instance = LoadBalanceService.roundRobin(compatibles);
                break;
            case LeastActive:
                instance = LoadBalanceService.leastActive(compatibles);
                break;
            case ConsistentHash:
                break;
        }

        return instance;

    }

}
