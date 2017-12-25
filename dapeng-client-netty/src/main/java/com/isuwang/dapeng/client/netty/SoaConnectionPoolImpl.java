package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.SoaConnection;
import com.isuwang.dapeng.core.SoaConnectionPool;
import com.isuwang.dapeng.registry.*;
import com.isuwang.dapeng.registry.zookeeper.ZkClientAgentImpl;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private   Map<String,ServiceZKInfo> zkInfos = new ConcurrentHashMap<>();
   // private Map<IpPort, SubPool> subPools = new ConcurrentHashMap<>();

    private Map<IpPort, SoaConnection> subPools = new ConcurrentHashMap<>();

    List<WeakReference<ClientInfo>> clientInfos;
    ZkClientAgent zkAgent = new ZkClientAgentImpl();

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
        // clientInfos.add(new WeakReference<ClientInfo>(client));

        zkAgent.syncService(serviceName, zkInfos);
        return null;
    }

    @Override
    public <REQ, RESP> RESP send(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws Exception {

        // step1：filter List[(ip,port, version)]
        // List[(service, version, ip, port)] getMatchedServices(service)

        ServiceZKInfo zkInfo = zkInfos.get(service);

        List<RuntimeInstance> compatibles = zkInfo.getRuntimeInstances().stream().filter(rt -> {
            return checkVersion(version, rt.version);
        }).collect(Collectors.toList());

        RuntimeInstance inst = loadbalance(compatibles);

        IpPort ipPort = new IpPort(inst.ip, inst.port);
        SoaConnection connection = subPools.get(ipPort);
        if(connection == null){
            connection = new SoaConnectionImpl(inst.ip,inst.port);
            subPools.put(ipPort,connection);
        }

        return connection.send(service, version, method, request, requestSerializer, responseSerializer);
    }

    private RuntimeInstance loadbalance(List<RuntimeInstance> compatibles) {
        return null;
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer, long timeout) throws Exception {
        return null;
    }

}
