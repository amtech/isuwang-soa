package com.isuwang.dapeng.core.definition;

import com.isuwang.dapeng.core.TCommonBeanSerializer;

/**
 * Created by lihuimin on 2017/12/14.
 */
public abstract class SoaFunctionDefinition<I, REQ, RESP>  {

    final String methodName;
    final TCommonBeanSerializer<REQ> reqSerializer;
    final TCommonBeanSerializer<RESP> respSerializer;

    public SoaFunctionDefinition(String methodName, TCommonBeanSerializer<REQ> reqSerializer, TCommonBeanSerializer<RESP> respSerializer) {
        this.methodName = methodName;
        this.reqSerializer = reqSerializer;
        this.respSerializer = respSerializer;
    }

    public abstract RESP apply(I iface, REQ req);

    public String getMethodName() {
        return methodName;
    }

    public TCommonBeanSerializer<REQ> getReqSerializer() {
        return reqSerializer;
    }

    public TCommonBeanSerializer<RESP> getRespSerializer() {
        return respSerializer;
    }

}
