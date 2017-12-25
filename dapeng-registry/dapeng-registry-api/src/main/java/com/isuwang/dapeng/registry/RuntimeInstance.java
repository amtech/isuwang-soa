package com.isuwang.dapeng.registry;

/**
 * Created by lihuimin on 2017/12/25.
 */
public class RuntimeInstance {

    public final String service;
    public final String version;
    public final String ip;
    public final int port;

    public RuntimeInstance(String service, String ip, int port, String version) {
        this.service = service;
        this.version = version;
        this.ip = ip;
        this.port = port;
    }
}
