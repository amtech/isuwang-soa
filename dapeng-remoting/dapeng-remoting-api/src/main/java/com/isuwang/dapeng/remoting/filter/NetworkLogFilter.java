package com.isuwang.dapeng.remoting.filter;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Network Log Filter
 *
 * @author craneding
 * @date 16/1/20
 */
public class NetworkLogFilter implements RemoteApiFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkLogFilter.class);

    @Override
    public void doFilter(FilterChain chain) throws TException {
        final SoaHeader soaHeader = (SoaHeader) chain.getAttribute(StubFilterChain.ATTR_KEY_HEADER);
        final Object request = chain.getAttribute(StubFilterChain.ATTR_KEY_REQUEST);

        final long startTime = System.currentTimeMillis();
        final InvocationContext context = (InvocationContext) chain.getAttribute(StubFilterChain.ATTR_KEY_CONTEXT);

        LOGGER.info("{} {} {} request", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName());

        try {
            chain.doFilter();
        } finally {
            // Object response = chain.getAttribute(StubFilterChain.ATTR_KEY_RESPONSE);
            final long endTime = System.currentTimeMillis();
            LOGGER.info("{} {} {} callee: {}:{} response respCode:{} time:{}ms", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(),
                    context.getCalleeIp(), context.getCalleePort(), soaHeader.getRespCode(), endTime-startTime);
        }
    }

    private static String formatToString(String msg) {
        if (null == msg)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (max_len < len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }

}
