package com.isuwang.dapeng.core.filter;

import com.isuwang.dapeng.core.SoaException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface FilterChain {

    // execute current filter's onEntry
    void onEntry(FilterContext ctx) throws SoaException;

    // execute current filter's onExit
    void onExit(FilterContext ctx)throws SoaException;


}
