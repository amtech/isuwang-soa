package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.SoaConnection;

import java.util.List;

/**
 * Created by lihuimin on 2017/12/25.
 */
public class SubPool {

    final String ip;
    final int port;

    //List<SoaConnection> connections;

    private SoaConnection connection;

    public SubPool(String ip, int port ) {
        this.ip = ip;
        this.port = port;
    }

    public SoaConnection getConnection() {
        if (connection == null){
            connection = new SoaConnectionImpl(ip,port);
        }
        return this.connection;
    }
}
