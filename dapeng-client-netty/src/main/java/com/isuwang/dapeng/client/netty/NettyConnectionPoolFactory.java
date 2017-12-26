package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.SoaConnectionPoolFactory;
import com.isuwang.dapeng.core.SoaConnectionPool;

/**
 * Created by lihuimin on 2017/12/24.
 */
public class NettyConnectionPoolFactory implements SoaConnectionPoolFactory {

    private static SoaConnectionPool pool;

    @Override
    public SoaConnectionPool getPool() {
        if (pool == null) {
            synchronized(SoaConnectionPoolImpl.class) {
                if (pool == null) {
                    pool = new SoaConnectionPoolImpl();
                }
            }
        }
        return pool;
    }

}
