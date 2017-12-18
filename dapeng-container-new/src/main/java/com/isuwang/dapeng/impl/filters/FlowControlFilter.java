package com.isuwang.dapeng.impl.filters;

import com.isuwang.dapeng.core.container.FilterChain;
import com.isuwang.dapeng.core.container.FilterContext;
import com.isuwang.dapeng.core.container.HandlerFilter;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class FlowControlFilter implements HandlerFilter {

    public void controlFlow(){}

    @Override
    public void onEntry(FilterContext ctx, FilterChain next) {

    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev) {

    }
}
