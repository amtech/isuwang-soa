package com.isuwang.dapeng.core.definition;

import com.isuwang.dapeng.core.TCommonBeanSerializer;

import java.util.concurrent.CompletableFuture;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaAsyncFunctionDefinition <I,REQ,RESP>{

    final String methodName;
    final TCommonBeanSerializer<REQ> reqSerializer;
    final TCommonBeanSerializer<RESP> respSerializer;


    public SoaAsyncFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
    }

    public abstract CompletableFuture<RESP> apply(I iface, REQ req);
}
