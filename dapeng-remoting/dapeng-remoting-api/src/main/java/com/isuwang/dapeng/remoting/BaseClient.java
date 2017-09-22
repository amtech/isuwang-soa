package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.registry.RegistryAgent;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.dapeng.remoting.filter.RemoteApiFilter;
import com.isuwang.dapeng.remoting.filter.StubFilterChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by tangliu on 17/7/20.
 */
public abstract class BaseClient {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseClient.class);

    protected String serviceName;
    protected String versionName;

    protected BaseClient(String serviceName, String versionName) {
        this.serviceName = serviceName;
        this.versionName = versionName;
    }

    protected static SoaConnectionPool connectionPool;

    static {
        try  {
            ServiceLoader<RemoteApiFilter> filterLoader = ServiceLoader.load(RemoteApiFilter.class,BaseClient.class.getClassLoader());
            // load filter
            for (RemoteApiFilter filter : filterLoader) {
                StubFilterChain.addFilter(filter);
            }
            // load connection pool
            ServiceLoader<SoaConnectionPool>soaConnectionPoolServiceLoader = ServiceLoader.load(SoaConnectionPool.class,BaseClient.class.getClassLoader());
            for(SoaConnectionPool soaConnectionPool : soaConnectionPoolServiceLoader){
                BaseClient.connectionPool = soaConnectionPool;
            }

        } catch (Exception e) {
            LOGGER.error("client load filter error", e);
        }

        if (!SoaSystemEnvProperties.SOA_REMOTING_MODE.equals("local")) {
            try  {

                ServiceLoader<RegistryAgent>registryAgentLoader = ServiceLoader.load(RegistryAgent.class,BaseClient.class.getClassLoader());
                for (RegistryAgent registryAgent : registryAgentLoader) {
                    RegistryAgentProxy.setCurrentInstance(RegistryAgentProxy.Type.Client,registryAgent);
                    RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Client).start();
                }
            } catch (Exception e) {
                LOGGER.error("client load registry error", e);
            }
        } else {
            LOGGER.info("soa remoting mode is {},client not load registry", SoaSystemEnvProperties.SOA_REMOTING_MODE);
        }

    }

    protected void initContext(String methodName) {
        InvocationContext context = InvocationContext.Factory.getCurrentInstance();

        context.setSeqid(getSeqId().incrementAndGet());

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

    protected abstract AtomicInteger getSeqId();

    protected boolean isSoaTransactionalProcess() {
        return false;
    }

    protected void destoryContext() {
        InvocationContext.Factory.removeCurrentInstance();
    }

}
