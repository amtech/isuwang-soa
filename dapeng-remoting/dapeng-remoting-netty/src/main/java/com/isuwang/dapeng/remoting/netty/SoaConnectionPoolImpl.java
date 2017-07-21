package com.isuwang.dapeng.remoting.netty;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaBaseCode;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.remoting.SoaConnection;
import com.isuwang.dapeng.remoting.SoaConnectionPool;
import com.isuwang.dapeng.remoting.SoaScalaConnection;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SoaConnectionPoolImpl implements SoaConnectionPool {

    private static final SoaConnectionPoolImpl pool = new SoaConnectionPoolImpl();

    static {
        IdleConnectionManager connectionManager = new IdleConnectionManager();
        connectionManager.start();
    }

    public static SoaConnectionPool getInstance() {
        return pool;
    }

    private Map<String, SoaConnectionImpl> connectionMap = new ConcurrentHashMap<>();

    private Map<String, SoaScalaConnectionImpl> scalaConnectionMap = new ConcurrentHashMap<>();

    @Override
    public synchronized SoaConnection getConnection() throws SoaException {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();

        if (context.getCalleeIp() == null || context.getCalleePort() <= 0)
            throw new SoaException(SoaBaseCode.NotFoundServer);

        String connectKey = context.getCalleeIp() + ":" + String.valueOf(context.getCalleePort());

        if (connectionMap.containsKey(connectKey)) {
            return connectionMap.get(connectKey);
        }

        SoaConnectionImpl soaConnection = new SoaConnectionImpl(context.getCalleeIp(), context.getCalleePort());

        connectionMap.put(connectKey, soaConnection);

        return soaConnection;
    }

    @Override
    public SoaScalaConnection getScalaConnection() throws SoaException {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();

        if (context.getCalleeIp() == null || context.getCalleePort() <= 0)
            throw new SoaException(SoaBaseCode.NotFoundServer);

        String connectKey = context.getCalleeIp() + ":" + String.valueOf(context.getCalleePort());

        if (scalaConnectionMap.containsKey(connectKey)) {
            return scalaConnectionMap.get(connectKey);
        }

        SoaScalaConnectionImpl soaConnection = new SoaScalaConnectionImpl(context.getCalleeIp(), context.getCalleePort());

        scalaConnectionMap.put(connectKey, soaConnection);

        return soaConnection;
    }

    /**
     * 删除链接
     *
     * @throws SoaException
     */
    @Override
    public synchronized void removeConnection() throws SoaException {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();

        if (context.getCalleeIp() == null || context.getCalleePort() <= 0)
            return;

        String connectKey = context.getCalleeIp() + ":" + String.valueOf(context.getCalleePort());

        if (connectionMap.containsKey(connectKey))
            connectionMap.remove(connectKey);

        if (scalaConnectionMap.containsKey(connectKey))
            scalaConnectionMap.remove(connectKey);
    }

}
