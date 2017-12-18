package com.isuwang.dapeng.api;


import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface HandlerFilter {

    void onEntry(FilterContext ctx, FilterChain next) throws TException;

    void onExit(FilterContext ctx, FilterChain prev)throws TException ;

}
