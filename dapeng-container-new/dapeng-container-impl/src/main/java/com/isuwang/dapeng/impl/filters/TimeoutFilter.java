package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.org.apache.thrift.TException;

public class TimeoutFilter implements Filter {

    @Override
    public void onEntry(FilterContext ctx, FilterChain next)  {

        try {
            next.onEntry(ctx);
        } catch (TException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev)  {
        // 第一个filter不需要调onExit

    }
}
