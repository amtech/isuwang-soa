package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.registry.ConfigKey;
import com.isuwang.dapeng.registry.RegistryAgent;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.dapeng.remoting.filter.SendMessageFilter;
import com.isuwang.dapeng.remoting.filter.StubFilterChain;
import com.isuwang.org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 基础客户端工具
 *
 * @author craneding
 * @date 15/9/24
 */
public class BaseScalaServiceClient {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseScalaServiceClient.class);

    public static final AtomicInteger seqid_ = new AtomicInteger(0);

    protected String serviceName;
    protected String versionName;

    protected BaseScalaServiceClient(String serviceName, String versionName) {
        this.serviceName = serviceName;
        this.versionName = versionName;
    }

    public SoaConnectionPool connectionPool = BaseClient.connectionPool;

    protected void initContext(String methodName) {
        InvocationContext context = InvocationContext.Factory.getCurrentInstance();

        context.setSeqid(seqid_.incrementAndGet());

        SoaHeader soaHeader = context.getHeader() == null ? new SoaHeader() : context.getHeader();

        InvocationContext.Factory.ISoaHeaderProxy headerProxy = InvocationContext.Factory.getSoaHeaderProxy();
        if (headerProxy != null) {
            soaHeader.setCallerFrom(headerProxy.callerFrom());
            soaHeader.setCustomerId(headerProxy.customerId());
            soaHeader.setCustomerName(headerProxy.customerName());
            soaHeader.setOperatorId(headerProxy.operatorId());
            soaHeader.setOperatorName(headerProxy.operatorName());
            soaHeader.setSessionId(headerProxy.sessionId());
        }

        //如果在容器内调用其它服务，将原始的调用者信息(customerId/customerName/operatorId/operatorName)传递
        if (TransactionContext.hasCurrentInstance()) {

            TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();
            SoaHeader oriHeader = transactionContext.getHeader();

            soaHeader.setCustomerId(oriHeader.getCustomerId());
            soaHeader.setCustomerName(oriHeader.getCustomerName());
            soaHeader.setOperatorId(oriHeader.getOperatorId());
            soaHeader.setOperatorName(oriHeader.getOperatorName());
            soaHeader.setSessionId(oriHeader.getSessionId());
        }

        soaHeader.setCallerIp(Optional.of(SoaSystemEnvProperties.SOA_CALLER_IP));
        soaHeader.setServiceName(serviceName);
        soaHeader.setMethodName(methodName);
        soaHeader.setVersionName(versionName);

        if (!soaHeader.getCallerFrom().isPresent())
            soaHeader.setCallerFrom(Optional.of(SoaSystemEnvProperties.SOA_SERVICE_CALLERFROM));


        if (!soaHeader.getSessionId().isPresent()) {
            soaHeader.setSessionId(Optional.of(UUID.randomUUID().toString()));
        }

        context.setHeader(soaHeader);

        if (context.getCalleeTimeout() <= 0)
            context.setCalleeTimeout(SoaSystemEnvProperties.SOA_SERVICE_TIMEOUT);

        context.setSoaTransactionProcess(isSoaTransactionalProcess());
    }

    protected boolean isSoaTransactionalProcess() {
        return false;
    }

    protected void destoryContext() {
        InvocationContext.Factory.removeCurrentInstance();
    }

    @SuppressWarnings("unchecked")
    protected <REQ, RESP> RESP sendBase(REQ request, TScalaBeanSerializer<REQ> requestSerializer, TScalaBeanSerializer<RESP> responseSerializer) throws TException {
        InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();

        final StubFilterChain stubFilterChain = new StubFilterChain();
        stubFilterChain.setLastFilter(new SendMessageFilter());

        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_CONTEXT, context);
        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_HEADER, soaHeader);
        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_REQUEST, request);
        stubFilterChain.setAttribute(SendMessageFilter.ATTR_KEY_SENDMESSAGE, (SendMessageFilter.SendMessageAction) (chain) -> {

            SoaScalaConnection conn = connectionPool.getScalaConnection();

            try {
                RESP resp = conn.send(request, requestSerializer, responseSerializer);
                chain.setAttribute(StubFilterChain.ATTR_KEY_RESPONSE, resp);
            } catch (SoaException e) {

                if (e.getCode().equals(SoaBaseCode.NotConnected.getCode()))
                    connectionPool.removeConnection();
                throw e;
            }
        });

        try {
            stubFilterChain.doFilter();
        } catch (SoaException e) {
            if (e.getCode().equals(SoaBaseCode.NotConnected.getCode()) || e.getCode().equals(SoaBaseCode.TimeOut.getCode())) {

                int failOverTimes = 0;
                String serviceKey = soaHeader.getServiceName() + "." + soaHeader.getVersionName() + "." + soaHeader.getMethodName() + ".consumer";
                RegistryAgent registryAgent = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client);

                Boolean usingFallbackZK = (Boolean) stubFilterChain.getAttribute(StubFilterChain.ATTR_KEY_USERING_FBZK);
                if (usingFallbackZK != null) {
                    Map<ConfigKey, Object> configs = registryAgent != null ? registryAgent.getConfig(usingFallbackZK, serviceKey) : null;
                    if (null != configs) {
                        failOverTimes = (Integer) configs.get(ConfigKey.FailOver);
                    }
                }

                if (context.getFailedTimes() < failOverTimes) {
                    context.setFailedTimes(context.getFailedTimes() + 1);
                    LOGGER.info("connect failed {} times, try again", context.getFailedTimes());
                    sendBase(request, requestSerializer, responseSerializer);
                } else
                    throw e;
            } else
                throw e;
        }

        return (RESP) stubFilterChain.getAttribute(StubFilterChain.ATTR_KEY_RESPONSE);
    }

    /**
     * 发送异步请求
     *
     * @param request            请求实体
     * @param response           返回实体
     * @param requestSerializer
     * @param responseSerializer
     * @param timeout            超时时间
     * @param <REQ>
     * @param <RESP>
     * @return
     * @throws TException
     */
    protected <REQ, RESP> Future<RESP> sendBaseAsync(REQ request, RESP response, TBeanSerializer<REQ> requestSerializer, TBeanSerializer<RESP> responseSerializer, long timeout) throws TException {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();
        soaHeader.setAsyncCall(true);

        final StubFilterChain stubFilterChain = new StubFilterChain();
        stubFilterChain.setLastFilter(new SendMessageFilter());

        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_CONTEXT, context);
        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_HEADER, soaHeader);
        stubFilterChain.setAttribute(StubFilterChain.ATTR_KEY_REQUEST, request);
        stubFilterChain.setAttribute(SendMessageFilter.ATTR_KEY_SENDMESSAGE, (SendMessageFilter.SendMessageAction) (chain) -> {
            SoaScalaConnection conn = connectionPool.getScalaConnection();
            Future<RESP> resp = conn.sendAsync(request, response, requestSerializer, responseSerializer, timeout);
            chain.setAttribute(StubFilterChain.ATTR_KEY_RESPONSE, resp);
        });

        stubFilterChain.doFilter();

        return (Future<RESP>) stubFilterChain.getAttribute(StubFilterChain.ATTR_KEY_RESPONSE);
    }

}
