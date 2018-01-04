package com.isuwang.dapeng.core.filter;


/**
 * Created by lihuimin on 2017/12/11.
 */
public interface Filter {

    void onEntry(FilterContext ctx, FilterChain next);

    void onExit(FilterContext ctx, FilterChain prev) ;

}
