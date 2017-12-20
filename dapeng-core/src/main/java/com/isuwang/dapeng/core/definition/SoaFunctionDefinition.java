package com.isuwang.dapeng.core.definition;

import com.isuwang.dapeng.core.TCommonBeanSerializer;

import java.util.concurrent.Future;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaFunctionDefinition<I, REQ, RESP>  {

    public static abstract class Sync<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Sync(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer){
            super(methodName, reqSerializer, respSerializer);
        }

        public abstract RESP apply(I iface, REQ req);
    }

    public static abstract class Async<I, REQ, RESP> extends SoaFunctionDefinition<I, REQ, RESP> {

        public Async(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer){
            super(methodName, reqSerializer, respSerializer);
        }

        public abstract Future<RESP> apply(I iface, REQ req);
    }

    public final String methodName;
    public final TCommonBeanSerializer<REQ> reqSerializer;
    public final TCommonBeanSerializer<RESP> respSerializer;

    public SoaFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
    }


}
