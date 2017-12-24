//package com.isuwang.dapeng.core.filter.container;
//
//import com.isuwang.dapeng.core.filter.Filter;
//import com.isuwang.dapeng.core.filter.FilterChain;
//import com.isuwang.org.apache.thrift.TException;
//
///**
// * Dispatch Filter
// *
// * @author craneding
// * @date 16/1/21
// */
//public class DispatchFilter implements Filter {
//
//    public static final String ATTR_KEY_CONTAINER_DISPATCH_ACTION = "container.dispatch.action";
//
//    public interface DispatchAction {
//
//        void doAction(FilterChain chain) throws TException;
//
//    }
//
//    @Override
//    public void doFilter(FilterChain chain) throws TException {
//        DispatchAction dispatchAction = (DispatchAction) chain.getAttribute(DispatchFilter.ATTR_KEY_CONTAINER_DISPATCH_ACTION);
//
//        dispatchAction.doAction(chain);
//    }
//
//}
