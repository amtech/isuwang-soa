package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.container.FilterChain;
import com.isuwang.dapeng.core.container.FilterContext;
import com.isuwang.dapeng.core.container.HandlerFilter;
import com.isuwang.org.apache.thrift.TException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class FilterChainDemo {

    static class ChainNode implements FilterChain {
        final HandlerFilter filter;
        final ChainNode next;
        FilterChain prev;

        public ChainNode(HandlerFilter filter, ChainNode next) {
            this.filter = filter;
            this.next = next;
            if(next != null) {
                next.prev = this;
            }
        }

        @Override
        public void onEntry(FilterContext ctx)throws TException {
            //ctx.push(filter);
            filter.onEntry(ctx, next);

        }

        @Override
        public void onExit(FilterContext ctx)throws TException {
            filter.onExit(ctx, prev);
        }
    }

    void test1()throws TException{

        HandlerFilter timeLog = null, soaReport = null,  reqRespLog = null;
        //FilterChain chain = null;
            // init -> TimeLog -> SoaReport -> ReqRespLog -> service method

            // new SharedChain( head, shared, tail )

        TransactionContext transactionContext = null; // init thread local

        final Method method = null; // REQ->RESP, REQ->Future[RESP]

        ChainNode init = null;
        //
        ChainNode last = new ChainNode(null, null) {
            @Override
            public void onEntry(FilterContext ctx) throws TException{
                try {
                    method.invoke(null);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }

                //
                onExit(ctx);
            }

            @Override
            public void onExit(FilterContext ctx)throws TException{
                if(prev != null) {
                    prev.onExit(ctx);
                }
            }
        };

        ChainNode chain = new ChainNode(timeLog, new ChainNode(soaReport, new ChainNode(reqRespLog, last)));
        // SharedChain chain = new SharedChain( filters, last )

        FilterContext ctx = null; // new FilterContext() ;
        chain.onEntry(ctx);


    }

}
