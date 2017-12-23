package com.isuwang.dapeng.container.filter;

import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.container.ContainerFilterChain;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network Times Filter
 *
 * @author craneding
 * @date 16/1/21
 */
public class ProviderTimesFilter implements ContainerFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProviderTimesFilter.class);

    @Override
    public void doFilter(FilterChain chain) throws TException {
        final long startTime = System.currentTimeMillis();
        final SoaHeader soaHeader = (SoaHeader) chain.getAttribute(ContainerFilterChain.ATTR_KEY_HEADER);
        final TransactionContext context = (TransactionContext) chain.getAttribute(ContainerFilterChain.ATTR_KEY_CONTEXT);

        try {
            chain.doFilter();
        } finally {
            LOGGER.info("{} {} {} {} 耗时:{}ms", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid(), System.currentTimeMillis() - startTime);
        }
    }

}
