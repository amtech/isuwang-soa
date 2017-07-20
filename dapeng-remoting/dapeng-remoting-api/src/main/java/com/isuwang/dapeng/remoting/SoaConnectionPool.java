package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.SoaException;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnectionPool {

    SoaConnection getConnection() throws SoaException;

    SoaScalaConnection getScalaConnection() throws SoaException;

    void removeConnection() throws SoaException;
}
