package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.SoaException;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnectionPool {

    @Deprecated
    SoaConnection getConnection() throws SoaException;

    SoaCommonConnection getCommonConnection() throws SoaException;

    void removeConnection() throws SoaException;
}
