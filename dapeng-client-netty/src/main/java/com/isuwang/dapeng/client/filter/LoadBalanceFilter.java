package com.isuwang.dapeng.client.filter;

import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by lihuimin on 2017/12/23.
 */
public class LoadBalanceFilter implements Filter {
    @Override
    public void onEntry(FilterContext ctx, FilterChain next){
        try {
            next.onEntry(ctx);
        } catch (TException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev)  {

        try {
            prev.onExit(ctx);
        } catch (TException e) {
            e.printStackTrace();
        }

    }
}
