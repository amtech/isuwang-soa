package com.isuwang.dapeng.impl.handler;

import java.util.Map;

/**
 * Created by lihuimin on 2017/12/14.
 */
public class SoaAsyncServiceDefinition <I>{

    //final I iface;    // async interface
    final Class<I> ifaceClass;

    final Map<String, SoaAsyncFunctionDefinition<I, ?, ?>> functions;

    SoaAsyncServiceDefinition(Class<I> ifaceClass, Map<String, SoaAsyncFunctionDefinition<I, ?, ?>> functions) {
//        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.functions = functions;
    }
}
