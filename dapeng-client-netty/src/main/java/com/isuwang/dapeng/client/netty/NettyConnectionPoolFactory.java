package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.SoaConnectionPoolFactory;
import com.isuwang.dapeng.core.SoaConnectionPool;

/**
 * Created by lihuimin on 2017/12/24.
 */
public class NettyConnectionPoolFactory implements SoaConnectionPoolFactory {

    private static SoaConnectionPool pool = new SoaConnectionPoolImpl();

    public static SoaConnectionPool getPool() {

        return pool;
    }

}
