package com.isuwang.dapeng.core;

import java.util.Map;

/**
 * Created by lihuimin on 2017/12/14.
 */
public class SoaServiceDefinition<I>{
    final I iface;    // sync interface
    private Class<I> ifaceClass;

    private Map<String, SoaFunctionDefinition<I,?,?>> functions;

    public SoaServiceDefinition(I iface,Class<I> ifaceClass, Map<String, SoaFunctionDefinition<I, ?, ?>> functions){
        this.iface = iface;
        this.ifaceClass = ifaceClass;
        this.functions = functions;
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> buildMap(SoaFunctionDefinition ...functions){
        return null;
    }

    public Map<String, SoaFunctionDefinition<I,?,?>> getFunctins (){
        return functions;
    }

    public I getIface() {
        return iface;
    }

    public Class<I> getIfaceClass() {
        return ifaceClass;
    }

    public void setIfaceClass(Class<I> ifaceClass) {
        this.ifaceClass = ifaceClass;
    }
}
