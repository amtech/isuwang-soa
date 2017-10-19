package com.isuwang.dapeng.remoting.filter;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.transaction.api.GlobalTransactionProcessTemplate;
import com.isuwang.org.apache.thrift.TException;

/**
 * Created by tangliu on 2016/4/11.
 */
public class SoaTransactionalProcessFilter implements RemoteApiFilter{
    @Override
    public void doFilter(FilterChain chain) throws TException {

        final InvocationContext context = (InvocationContext) chain.getAttribute(StubFilterChain.ATTR_KEY_CONTEXT);

        if (SoaSystemEnvProperties.SOA_TRANSACTIONAL_ENABLE && TransactionContext.hasCurrentInstance() && TransactionContext.Factory.getCurrentInstance().getCurrentTransactionId() > 0 && context.isSoaTransactionProcess()) {// in container and is a transaction process
            Object req = chain.getAttribute(StubFilterChain.ATTR_KEY_REQUEST);

            new GlobalTransactionProcessTemplate<>(req).execute(() -> {
                chain.doFilter();
                return chain.getAttribute(StubFilterChain.ATTR_KEY_RESPONSE);
            });
        } else {
            chain.doFilter();
        }
    }
}
