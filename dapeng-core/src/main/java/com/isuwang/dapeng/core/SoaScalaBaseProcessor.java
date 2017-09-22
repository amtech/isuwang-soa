package com.isuwang.dapeng.core;

import com.isuwang.dapeng.core.filter.container.ContainerFilterChain;
import com.isuwang.dapeng.core.filter.container.DispatchFilter;
import com.isuwang.dapeng.core.log.LogUtil;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.TProcessor;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Soa基础处理器
 *
 * @author craneding
 * @date 15/9/18
 */
public class SoaScalaBaseProcessor<I> implements TProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaScalaBaseProcessor.class);

    private final I iface;
    private Class<I> interfaceClass;
    private final Map<String, SoaProcessFunction<I, ?, ?, ? extends TScalaBeanSerializer<?>, ? extends TScalaBeanSerializer<?>>> processMap;

    public SoaScalaBaseProcessor(I iface, Map<String, SoaProcessFunction<I, ?, ?, ? extends TScalaBeanSerializer<?>, ? extends TScalaBeanSerializer<?>>> processMap) {
        this.iface = iface;
        this.processMap = processMap;
    }

    @Override
    public I getIface() {
        return iface;
    }

    @Override
    public boolean process(TProtocol in, TProtocol out) throws TException {
        // threadlocal
        TransactionContext context = TransactionContext.Factory.getCurrentInstance();
        SoaHeader soaHeaderOrigin = context.getHeader();
        String methodName = soaHeaderOrigin.getMethodName();

        final String logId = soaHeaderOrigin.getServiceName() + "/" + soaHeaderOrigin.getMethodName();

        ContainerFilterChain filterChain = new ContainerFilterChain();
        filterChain.setLastFilter(new DispatchFilter());

        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_LOGID, logId);
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_CONTEXT, context);
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_HEADER, soaHeaderOrigin);
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_IFACE, iface);
        filterChain.setAttribute(DispatchFilter.ATTR_KEY_CONTAINER_DISPATCH_ACTION, (DispatchFilter.DispatchAction) chain -> {

            // read
            //TMessage tMessage = in.readMessageBegin();
            @SuppressWarnings("unchecked")
            SoaProcessFunction<I, Object, Object, ? extends TScalaBeanSerializer<Object>, ? extends TScalaBeanSerializer<Object>> soaProcessFunction = (SoaProcessFunction<I, Object, Object, ? extends TScalaBeanSerializer<Object>, ? extends TScalaBeanSerializer<Object>>) getProcessMapView().get(methodName);
            if (soaProcessFunction == null)
                throw new SoaException("系统错误", "方法(" + methodName + ")不存在");
            Object args = soaProcessFunction.getReqSerializer().read(in);
            in.readMessageEnd();

            SoaHeader soaHeader = (SoaHeader) chain.getAttribute(ContainerFilterChain.ATTR_KEY_HEADER);
            if (!soaHeader.getSessionId().isPresent()) {
                soaHeader.setSessionId(Optional.of(UUID.randomUUID().toString()));
            }

            //LOGGER.info("{} {} {} {} request header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid(), soaHeader.toString(), formatToString(soaProcessFunction.getReqSerializer().toString(args)));
            LogUtil.logInfo(SoaScalaBaseProcessor.class,soaHeader,"{} {} {} operatorId:{} operatorName:{} response body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid().toString(), soaHeader.toString(), formatToString(soaProcessFunction.getResSerializer().toString(args)));
            LogUtil.logDebug(SoaScalaBaseProcessor.class,soaHeader,"{} {} {} {} response header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(), formatToString(soaProcessFunction.getResSerializer().toString(args)));

            long startTime = System.currentTimeMillis();


            Object result = null;
            try {
                result = soaProcessFunction.getResult(iface, args);
                //LOGGER.info("{} {} {} {} response header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid(), soaHeader.toString(), formatToString(soaProcessFunction.getResSerializer().toString(result)));
                LogUtil.logInfo(SoaScalaBaseProcessor.class,soaHeader,"{} {} {} operatorId:{} operatorName:{} response body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid().toString(), soaHeader.toString(), formatToString(soaProcessFunction.getResSerializer().toString(result)));
                LogUtil.logDebug(SoaScalaBaseProcessor.class,soaHeader,"{} {} {} {} response header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(), formatToString(soaProcessFunction.getResSerializer().toString(result)));
            } finally {
                chain.setAttribute(ContainerFilterChain.ATTR_KEY_I_PROCESSTIME, System.currentTimeMillis() - startTime);

                soaHeader.setAttachment("dapeng_args", args.toString());
                soaHeader.setAttachment("dapeng_result", result == null ? "null" : result.toString());
            }
            // write
            context.getHeader().setRespCode(Optional.of("0000"));
            context.getHeader().setRespMessage(Optional.of("成功"));
            out.writeMessageBegin(new TMessage(context.getHeader().getMethodName(), TMessageType.CALL, context.getSeqid()));
            soaProcessFunction.getResSerializer().write(result, out);
            out.writeMessageEnd();

        });

        filterChain.doFilter();

        return true;
    }


    /**
     * 异步调用过程
     *
     * @param in
     * @param out
     * @return
     * @throws TException
     */
    public CompletableFuture<Context> processAsync(TProtocol in, TProtocol out) throws TException {

        final CompletableFuture<Context> futureResult = new CompletableFuture<>();
        // threadlocal
        TransactionContext context = TransactionContext.Factory.getCurrentInstance();
        String methodName = context.getHeader().getMethodName();

        final String logId = context.getHeader().getServiceName() + "/" + context.getHeader().getMethodName();

        ContainerFilterChain filterChain = new ContainerFilterChain();
        filterChain.setLastFilter(new DispatchFilter());

        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_LOGID, logId);
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_CONTEXT, context);
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_HEADER, context.getHeader());
        filterChain.setAttribute(ContainerFilterChain.ATTR_KEY_IFACE, iface);
        filterChain.setAttribute(DispatchFilter.ATTR_KEY_CONTAINER_DISPATCH_ACTION, (DispatchFilter.DispatchAction) chain -> {

            @SuppressWarnings("unchecked")
            SoaProcessFunction<I, Object, Object, ? extends TScalaBeanSerializer<Object>, ? extends TScalaBeanSerializer<Object>> soaProcessFunction = (SoaProcessFunction<I, Object, Object, ? extends TScalaBeanSerializer<Object>, ? extends TScalaBeanSerializer<Object>>) getProcessMapView().get(methodName);
            Object args = soaProcessFunction.getReqSerializer().read(in);
            in.readMessageEnd();

            SoaHeader soaHeader = (SoaHeader) chain.getAttribute(ContainerFilterChain.ATTR_KEY_HEADER);
            LOGGER.info("{} {} {} {} request header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid(), soaHeader.toString(), formatToString(soaProcessFunction.getReqSerializer().toString(args)));
            long startTime = System.currentTimeMillis();

            try {
                CompletableFuture<Object> future = (CompletableFuture) soaProcessFunction.getResultAsync(iface, args);
                future.whenComplete((realResult, ex) -> {
                    if (realResult != null) {
                        AsyncAccept(context, soaProcessFunction, realResult, out, futureResult);
                    } else {
                        TransactionContext.Factory.setCurrentInstance(context);
                        futureResult.completeExceptionally(ex);
                    }
                });
            } finally {
                chain.setAttribute(ContainerFilterChain.ATTR_KEY_I_PROCESSTIME, System.currentTimeMillis() - startTime);
            }

        });

        filterChain.doFilter();

        return futureResult;
    }

    /**
     * 异步处理，当返回结果被complete时调用
     *
     * @param context
     * @param soaProcessFunction
     * @param result
     * @param out
     * @param future
     */
    private void AsyncAccept(Context context, SoaProcessFunction<I, Object, Object, ? extends TScalaBeanSerializer<Object>, ? extends TScalaBeanSerializer<Object>> soaProcessFunction, Object result, TProtocol out, CompletableFuture future) {

        try {
            TransactionContext.Factory.setCurrentInstance((TransactionContext) context);
            SoaHeader soaHeader = context.getHeader();
            LOGGER.info("{} {} {} {} response header:{} body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), context.getSeqid(), soaHeader.toString(), formatToString(soaProcessFunction.getResSerializer().toString(result)));

            soaHeader.setRespCode(Optional.of("0000"));
            soaHeader.setRespMessage(Optional.of("成功"));
            out.writeMessageBegin(new TMessage(soaHeader.getMethodName(), TMessageType.CALL, context.getSeqid()));
            soaProcessFunction.getResSerializer().write(result, out);
            out.writeMessageEnd();
            /**
             * 通知外层handler处理结果
             */
            future.complete(context);
        } catch (TException e) {
            e.printStackTrace();
        }

    }

    private static String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }

    @Override
    public Map<String, SoaProcessFunction<I, ?, ?, ? extends TScalaBeanSerializer<?>, ? extends TScalaBeanSerializer<?>>> getProcessMapView() {
        return Collections.unmodifiableMap(processMap);
    }

    @Override
    public Class<I> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public void setInterfaceClass(Class interfaceClass) {
        this.interfaceClass = interfaceClass;
    }

}
