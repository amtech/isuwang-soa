package com.isuwang.dapeng.impl.plugins.netty;

import com.isuwang.dapeng.core.TCommonBeanSerializer;

import java.util.concurrent.Future;

/**
 * Created by lihuimin on 2017/12/14.
 */
abstract class SoaAsyncFunctionDefinition <I,REQ,RESP>{

    final String methodName;
    final TCommonBeanSerializer<REQ> reqSerializer;
    final TCommonBeanSerializer<RESP> respSerializer;


    public SoaAsyncFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer, boolean isSync) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
    }

    public abstract Future<RESP> apply(I iface, REQ req);
}
