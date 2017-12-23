package com.isuwang.dapeng.remoting.filter;

import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.registry.ServiceInfo;
import com.isuwang.org.apache.thrift.TException;

/**
 * Send Message Filter
 *
 * @author craneding
 * @date 16/1/20
 */
public class SendMessageFilter implements Filter {

    public static final String ATTR_KEY_SENDMESSAGE = "send.message.action";

    public interface SendMessageAction {

        void doAction(FilterChain chain) throws TException;

    }

    @Override
    public void doFilter(FilterChain chain) throws TException {
        //add active count
        ServiceInfo serviceInfo = (ServiceInfo) chain.getAttribute(StubFilterChain.ATTR_KEY_SERVERINFO);

        if (serviceInfo != null)
            serviceInfo.getActiveCount().incrementAndGet();

        try {
            SendMessageAction action = (SendMessageAction) chain.getAttribute(SendMessageFilter.ATTR_KEY_SENDMESSAGE);

            action.doAction(chain);
        } finally {
            if (serviceInfo != null)
                serviceInfo.getActiveCount().decrementAndGet();
        }

    }

}
