package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class HandlerFilterChain implements FilterChain {

    final Filter filter;
    FilterChain next;
    FilterChain prev;

    public HandlerFilterChain(Filter filter, FilterChain next) {
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
