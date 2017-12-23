package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.org.apache.thrift.TException;

public class TimeoutFilter implements Filter {

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) throws TException {

        next.onEntry(ctx);


    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) throws TException {

    }
}
