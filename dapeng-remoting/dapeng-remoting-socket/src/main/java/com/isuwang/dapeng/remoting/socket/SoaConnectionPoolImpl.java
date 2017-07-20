package com.isuwang.dapeng.remoting.socket;

import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.remoting.SoaConnection;
import com.isuwang.dapeng.remoting.SoaConnectionPool;
import com.isuwang.dapeng.remoting.SoaScalaConnection;

/**
 * Soa连接池
 *
 * @author craneding
 * @date 15/8/5
 */
public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private static final SoaConnectionPoolImpl pool = new SoaConnectionPoolImpl();

    public static SoaConnectionPool getInstance() {
        return pool;
    }

    @Override
    public SoaConnection getConnection() {
        return new SoaConnectionImpl();
    }

    @Override
    public SoaScalaConnection getScalaConnection() throws SoaException {
        return null;
    }

    @Override
    public void removeConnection() throws SoaException {

    }

}
