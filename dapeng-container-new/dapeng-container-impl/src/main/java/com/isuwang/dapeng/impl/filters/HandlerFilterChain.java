package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.api.FilterChain;
import com.isuwang.dapeng.api.FilterContext;
import com.isuwang.dapeng.api.HandlerFilter;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class HandlerFilterChain implements FilterChain {

    final HandlerFilter filter;
    FilterChain next;
    FilterChain prev;

    public HandlerFilterChain(HandlerFilter filter, FilterChain next) {
        this.filter = filter;
        this.next = next;
    }

    @Override
    public void onEntry(FilterContext ctx) throws TException{
        filter.onEntry(ctx, next);
    }

    @Override
    public void onExit(FilterContext ctx)throws TException {
        filter.onExit(ctx, prev);
    }
}
