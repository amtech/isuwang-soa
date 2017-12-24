package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.SoaConnection;
import com.isuwang.dapeng.core.SoaConnectionPool;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    class SubPool {
        final String ip;
        final int port;

        List<SoaConnection> connections;

        SubPool(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }

        SoaConnection getConnection() {
            // TODO
            return null;
        }
    }

    class RuntimeInstance {
        final String service;
        final String version;
        final String ip;
        final int port;

        RuntimeInstance(String service, String version, String ip, int port) {
            this.service = service;
            this.version = version;
            this.ip = ip;
            this.port = port;
        }
    }
    class ServiceZKInfo {
        final String service;

        List<RuntimeInstance> runtimeInstances ;
        Properties config;

        ServiceZKInfo(String service) {
            this.service = service;
        }
    }


    interface ZKAgent {
        void syncService(String service, ServiceZKInfo info);
        void cancnelSyncService(String service);
    }

    class IpPort{
        final String ip;
        final int port;

        IpPort(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    Map<String, ServiceZKInfo> zkInfos;
    Map<IpPort, SubPool> subPools;

    List<WeakReference<ClientInfo>> clientInfos;
    ZKAgent zkAgent = null;

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

        // zkAgent.syncService(serviceName, null);
        return null;
    }

    @Override
    public <REQ, RESP> RESP send(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws Exception {

        // step1：filter List[(ip,port, version)]
        // List[(service, version, ip, port)] getMatchedServices(service)

        ServiceZKInfo zkInfo = zkInfos.get(service);

        List<RuntimeInstance> compatibles = zkInfo.runtimeInstances.stream().filter(rt -> {
            return checkVersion(version, rt.version);
        }).collect(Collectors.toList());

        RuntimeInstance inst = loadbalance(compatibles);

        SubPool pool = subPools.get(new IpPort(inst.ip, inst.port));

        SoaConnection connection = pool.getConnection();

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
