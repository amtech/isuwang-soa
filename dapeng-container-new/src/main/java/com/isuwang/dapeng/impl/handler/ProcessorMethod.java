package com.isuwang.dapeng.impl.handler;

import java.util.concurrent.CompletableFuture;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class ProcessorMethod<REQ,RESP> {

    private String methodName;

    public RESP invoke(REQ request){
        RESP response =null;
        return response;
    }

    public CompletableFuture<RESP> invokeAsync(REQ request){

        CompletableFuture<RESP> response = null;
        return response;
    }

}
