package com.isuwang.dapeng.impl.handler;

import com.isuwang.dapeng.core.TCommonBeanSerializer;

import java.util.concurrent.CompletableFuture;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaFunctionDefinition<I, REQ, RESP>  {

    final String methodName;
    final TCommonBeanSerializer<REQ> reqSerializer;
    final TCommonBeanSerializer<RESP> respSerializer;
    final boolean isAsync;

    public SoaFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer, boolean isASync) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
        this.isAsync=isASync;
    }

    public abstract RESP apply(I iface, REQ req);

    public abstract CompletableFuture<RESP> applyAsync(I iface, REQ req);

    public String getMethodName() {
        return methodName;
    }

    public TCommonBeanSerializer<REQ> getReqSerializer() {
        return reqSerializer;
    }

    public TCommonBeanSerializer<RESP> getRespSerializer() {
        return respSerializer;
    }

    public boolean isAsync() {
        return isAsync;
    }
}
