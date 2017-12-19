package com.isuwang.dapeng.core.definition;

import java.util.Map;

/**
 * Created by lihuimin on 2017/12/14.
 */
public class SoaAsyncServiceDefinition <I>{

    final I iface;    // async interface
    final Class<I> ifaceClass;

    final Map<String, SoaAsyncFunctionDefinition<I, ?, ?>> functions;

    public SoaAsyncServiceDefinition(I iface,Class<I> ifaceClass, Map<String, SoaAsyncFunctionDefinition<I, ?, ?>> functions) {
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.functions = functions;
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> buildMap(SoaAsyncFunctionDefinition ...functions){
        return null;
    }


    public I getIface() {
        return iface;
    }

    public Class<I> getIfaceClass() {
        return ifaceClass;
    }

    public Map<String, SoaAsyncFunctionDefinition<I, ?, ?>> getFunctions() {
        return functions;
    }
}
