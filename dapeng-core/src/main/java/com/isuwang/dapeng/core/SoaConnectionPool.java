package com.isuwang.dapeng.core;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnectionPool {

    SoaConnection getConnection() throws SoaException;

    void removeConnection(SoaConnection connection) throws SoaException;
}
