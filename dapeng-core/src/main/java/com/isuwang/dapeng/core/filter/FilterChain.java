package com.isuwang.dapeng.core.filter;

import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface FilterChain {

    // execute current filter's onEntry
    void onEntry(FilterContext ctx) throws TException;

    // execute current filter's onExit
    void onExit(FilterContext ctx)throws TException;


}
