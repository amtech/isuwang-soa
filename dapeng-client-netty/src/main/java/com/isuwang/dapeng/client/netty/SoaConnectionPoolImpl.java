package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.SoaConnection;
import com.isuwang.dapeng.core.SoaConnectionPool;
import com.isuwang.dapeng.core.SoaException;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private SoaConnection connection;

    @Override
    public SoaConnection getConnection() throws SoaException {
        SoaConnectionImpl soaConnection = new SoaConnectionImpl("127.0.0.1", 9090);
        return soaConnection;
    }

    @Override
    public void removeConnection(SoaConnection connection) throws SoaException {

    }
}
