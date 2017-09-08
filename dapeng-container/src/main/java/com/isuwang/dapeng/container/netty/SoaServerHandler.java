package com.isuwang.dapeng.container.netty;

import com.isuwang.dapeng.container.util.LoggerUtil;
import com.isuwang.dapeng.container.util.PlatformProcessDataFactory;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.log.LogUtil;
import com.isuwang.dapeng.monitor.api.domain.PlatformProcessData;
import com.isuwang.dapeng.registry.ConfigKey;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.TProcessor;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Soa Server Handler
 *
 * @author craneding
 * @date 16/1/12
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);
    private static final Logger SIMPLE_LOGGER = LoggerFactory.getLogger(LoggerUtil.SIMPLE_LOG);

    private static Map<ProcessorKey, TProcessor<?>> soaProcessors;

    private final Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;

    static class ServerThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        ServerThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "trans-pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }

    private volatile static ExecutorService executorService = Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE, new ServerThreadFactory());

    public SoaServerHandler(Map<ProcessorKey, TProcessor<?>> soaProcessors) {
        this.soaProcessors = soaProcessors;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        readRequestHeader(ctx, (ByteBuf) msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }

    protected void readRequestHeader(ChannelHandlerContext ctx, ByteBuf inputBuf) throws TException {
        TSoaTransport inputSoaTransport = null;

        boolean intoPool = false, intoProcessRequest = false, isAsync = false;
        try {
            final Long startTime = System.currentTimeMillis();

            final int requestLength = getRequestLength(inputBuf);

            final TransactionContext context = TransactionContext.Factory.getNewInstance();
            final SoaHeader soaHeader = new SoaHeader();
            inputSoaTransport = new TSoaTransport(inputBuf);
            context.setHeader(soaHeader);
            TransactionContext.Factory.setCurrentInstance(context);

            final PlatformProcessData processData = PlatformProcessDataFactory.getNewInstance(soaHeader);
            processData.setRequestFlow(requestLength + Integer.BYTES);
            PlatformProcessDataFactory.setCurrentInstance(processData);

            final TSoaServiceProtocol inputProtocol = new TSoaServiceProtocol(inputSoaTransport, false);
            TMessage tMessage = inputProtocol.readMessageBegin();
            context.setSeqid(tMessage.seqid);

            /**
             * check if use executorService for this service and
             */
            boolean b = true;

            String serviceKey = soaHeader.getServiceName() + "." + soaHeader.getVersionName() + "." + soaHeader.getMethodName() + ".producer";
            Map<ConfigKey, Object> configs = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server).getConfig(false, serviceKey);

            if (null != configs) {
                Boolean aBoolean = (Boolean) configs.get(ConfigKey.ThreadPool);

                if (aBoolean != null)
                    b = aBoolean.booleanValue();
            }

            intoProcessRequest = true;

            if (useThreadPool && b) {
                final TSoaTransport finalInputSoaTransport = inputSoaTransport;

                if (soaHeader.isAsyncCall()) {
                    isAsync = true;
                    executorService.execute(() -> processRequestAsync(ctx, inputBuf, finalInputSoaTransport, inputProtocol, context, startTime, processData));
                } else
                    executorService.execute(() -> processRequest(ctx, inputBuf, finalInputSoaTransport, inputProtocol, context, startTime, processData));

                intoPool = true;

            } else {

                if (soaHeader.isAsyncCall()) {
                    isAsync = true;
                    processRequestAsync(ctx, inputBuf, inputSoaTransport, inputProtocol, context, startTime, processData);
                } else
                    processRequest(ctx, inputBuf, inputSoaTransport, inputProtocol, context, startTime, processData);
            }

        } finally {
            if (!intoPool && !isAsync) {
                if (inputSoaTransport.isOpen())
                    inputSoaTransport.close();
            }

            if (!intoProcessRequest)
                inputBuf.release();

            TransactionContext.Factory.removeCurrentInstance();
            PlatformProcessDataFactory.removeCurrentInstance();
        }
    }

    private int getRequestLength(ByteBuf inputBuf) {
        int readerIndex = inputBuf.readerIndex();
        int requestLength = inputBuf.readInt();
        inputBuf.readerIndex(readerIndex);
        return requestLength;
    }

    protected void processRequestAsync(ChannelHandlerContext ctx, ByteBuf inputBuf, TSoaTransport inputSoaTransport, TSoaServiceProtocol inputProtocol, TransactionContext context, Long startTime, PlatformProcessData processData) {

        final long waitingTime = System.currentTimeMillis() - startTime;

        final ByteBuf outputBuf = ctx.alloc().buffer(8192);

        TransactionContext.Factory.setCurrentInstance(context);
        PlatformProcessDataFactory.setCurrentInstance(processData);

        SoaHeader soaHeader = context.getHeader();

        final TSoaTransport outputSoaTransport = new TSoaTransport(outputBuf);
        final TSoaServiceProtocol outputProtocol = new TSoaServiceProtocol(outputSoaTransport, false);

        try {
            TProcessor<?> soaProcessor = soaProcessors.get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));
            if (soaProcessor == null) {
                throw new SoaException(SoaBaseCode.NotFoundServer);
            }
            CompletableFuture<Context> future = soaProcessor.processAsync(inputProtocol, outputProtocol);
            future.whenComplete((resultContext, ex) -> {

                if (resultContext != null) {
                    String responseCode = "-", responseMsg = "-";
                    try {
                        outputSoaTransport.flush();
                        ctx.writeAndFlush(outputBuf);

                        if (resultContext.getHeader().getRespCode().isPresent())
                            responseCode = resultContext.getHeader().getRespCode().get();
                        if (resultContext.getHeader().getRespMessage().isPresent())
                            responseMsg = resultContext.getHeader().getRespMessage().get();

                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (inputSoaTransport != null)
                            inputSoaTransport.close();

                        if (outputSoaTransport != null)
                            outputSoaTransport.close();

                        final String fRspCode = responseCode;
                        final String fRspMsg = responseMsg;

                        PlatformProcessDataFactory.update(soaHeader, cacheProcessData -> {
                            long totalTime = System.currentTimeMillis() - startTime;

                            if (cacheProcessData.getPMinTime() == 0 || totalTime < cacheProcessData.getPMinTime())
                                cacheProcessData.setPMinTime(totalTime);
                            if (cacheProcessData.getPMaxTime() == 0 || totalTime > cacheProcessData.getPMaxTime())
                                cacheProcessData.setPMaxTime(totalTime);
                            cacheProcessData.setPTotalTime(cacheProcessData.getPTotalTime() + totalTime);

                            if (fRspCode.equals("0000"))
                                cacheProcessData.setSucceedCalls(cacheProcessData.getSucceedCalls() + 1);
                            else
                                cacheProcessData.setFailCalls(cacheProcessData.getFailCalls() + 1);

                            cacheProcessData.setTotalCalls(cacheProcessData.getTotalCalls() + 1);
                            cacheProcessData.setRequestFlow(cacheProcessData.getRequestFlow() + processData.getRequestFlow());
                            cacheProcessData.setResponseFlow(cacheProcessData.getResponseFlow() + outputBuf.writerIndex());

                            StringBuilder builder = new StringBuilder("DONE")
                                    .append(" ").append(ctx.channel().remoteAddress())
                                    .append(" ").append(ctx.channel().localAddress())
                                    .append(" ").append(context.getSeqid())
                                    .append(" ").append(soaHeader.getServiceName()).append(".").append(soaHeader.getMethodName()).append(":").append(soaHeader.getVersionName())
                                    .append(" ").append(fRspCode)
                                    .append(" ").append(fRspMsg)
                                    .append(" ").append(processData.getRequestFlow())
                                    .append(" ").append(outputBuf.writerIndex())
                                    .append(" ").append(waitingTime).append("ms")
                                    .append(" ").append(totalTime).append("ms");
                            SIMPLE_LOGGER.info(builder.toString());
                        });
                    }
                } else {
                    LOGGER.error(ex.getMessage(), ex);
                    if (ex instanceof SoaException) {
                        writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, (SoaException) ex);
                    } else {
                        String errMsg = ex.getCause() != null ? ex.getCause().toString() : (ex.getMessage() != null ? ex.getMessage().toString() : SoaBaseCode.UnKnown.getMsg());
                        writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, new SoaException(SoaBaseCode.UnKnown, errMsg));
                    }
                }
            });

        } catch (SoaException e) {
            LOGGER.error(e.getMessage(), e);

            writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, e);

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);

            String errMsg = e.getCause() != null ? e.getCause().toString() : (e.getMessage() != null ? e.getMessage().toString() : SoaBaseCode.UnKnown.getMsg());
            writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, new SoaException(SoaBaseCode.UnKnown, errMsg));

        } finally {

            inputBuf.release();

            TransactionContext.Factory.removeCurrentInstance();
            PlatformProcessDataFactory.removeCurrentInstance();
        }

    }


    protected void processRequest(ChannelHandlerContext ctx, ByteBuf inputBuf, TSoaTransport inputSoaTransport, TSoaServiceProtocol inputProtocol, TransactionContext context, Long startTime, PlatformProcessData processData) {

        final long waitingTime = System.currentTimeMillis() - startTime;

        final ByteBuf outputBuf = ctx.alloc().buffer(8192);

        TransactionContext.Factory.setCurrentInstance(context);
        PlatformProcessDataFactory.setCurrentInstance(processData);

        SoaHeader soaHeader = context.getHeader();

        final TSoaTransport outputSoaTransport = new TSoaTransport(outputBuf);
        TSoaServiceProtocol outputProtocol = null;

        boolean isSucceed = false;

        String responseCode = "-", responseMsg = "-";
        try {
            outputProtocol = new TSoaServiceProtocol(outputSoaTransport, false);
            TProcessor<?> soaProcessor = soaProcessors.get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (soaProcessor == null) {
                throw new SoaException(SoaBaseCode.NotFoundServer);
            }

            soaProcessor.process(inputProtocol, outputProtocol);

            outputSoaTransport.flush();

            ctx.writeAndFlush(outputBuf);

            isSucceed = true;

            if (soaHeader.getRespCode().isPresent())
                responseCode = soaHeader.getRespCode().get();
            if (soaHeader.getRespMessage().isPresent())
                responseMsg = soaHeader.getRespMessage().get();
        } catch (SoaException e) {

            //LOGGER.error(e.getMessage(), e);
            LogUtil.logError(SoaServerHandler.class,soaHeader,e.getMessage());

            writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, e);

            responseCode = e.getCode();
            responseMsg = e.getMsg();
        } catch (Throwable e) {
            //LOGGER.error(e.getMessage(), e);
            LogUtil.logError(SoaServerHandler.class,soaHeader,e.getMessage());
            String errMsg = e.getCause() != null ? e.getCause().toString() : (e.getMessage() != null ? e.getMessage().toString() : e.toString());
            writeErrorMessage(ctx, outputBuf, context, soaHeader, outputSoaTransport, outputProtocol, new SoaException(SoaBaseCode.UnKnown, errMsg));

            responseCode = SoaBaseCode.UnKnown.getCode();
            responseMsg = errMsg;
        } finally {
            if (inputSoaTransport != null)
                inputSoaTransport.close();

            if (outputSoaTransport != null)
                outputSoaTransport.close();

            //LEAK: ByteBuf.release() was not called before it's garbage-collected. Enable advanced leak reporting to find out where the leak occurred. To enable advanced leak reporting, specify the JVM option '-Dio.netty.leakDetectionLevel=advanced' or call ResourceLeakDetector.setLevel() See http://netty.io/wiki/reference-counted-objects.html for more information.
            // to see SoaDecoder: ByteBuf msg = in.slice(readerIndex, length + Integer.BYTES).retain();
            inputBuf.release();

            final boolean finalIsSucceed = isSucceed;
            final String finalResponseCode = responseCode;
            final String finalResponseMsg = responseMsg;
            PlatformProcessDataFactory.update(soaHeader, cacheProcessData -> {
                long totalTime = System.currentTimeMillis() - startTime;

                if (cacheProcessData.getPMinTime() == 0 || totalTime < cacheProcessData.getPMinTime())
                    cacheProcessData.setPMinTime(totalTime);
                if (cacheProcessData.getPMaxTime() == 0 || totalTime > cacheProcessData.getPMaxTime())
                    cacheProcessData.setPMaxTime(totalTime);
                cacheProcessData.setPTotalTime(cacheProcessData.getPTotalTime() + totalTime);

                if (finalIsSucceed)
                    cacheProcessData.setSucceedCalls(cacheProcessData.getSucceedCalls() + 1);
                else
                    cacheProcessData.setFailCalls(cacheProcessData.getFailCalls() + 1);

                cacheProcessData.setTotalCalls(cacheProcessData.getTotalCalls() + 1);

                cacheProcessData.setRequestFlow(cacheProcessData.getRequestFlow() + processData.getRequestFlow());
                cacheProcessData.setResponseFlow(cacheProcessData.getResponseFlow() + outputBuf.writerIndex());

                StringBuilder builder = new StringBuilder("DONE")
                        .append(" ").append(ctx.channel().remoteAddress())
                        .append(" ").append(ctx.channel().localAddress())
                        .append(" ").append(context.getSeqid())
                        .append(" ").append(soaHeader.getServiceName()).append(".").append(soaHeader.getMethodName()).append(":").append(soaHeader.getVersionName())
                        .append(" ").append(finalResponseCode)
                        .append(" ").append(finalResponseMsg)
                        .append(" ").append(processData.getRequestFlow())
                        .append(" ").append(outputBuf.writerIndex())
                        .append(" ").append(waitingTime).append("ms")
                        .append(" ").append(totalTime).append("ms");
                SIMPLE_LOGGER.info(builder.toString());
            });

            TransactionContext.Factory.removeCurrentInstance();
            PlatformProcessDataFactory.removeCurrentInstance();
        }
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, ByteBuf outputBuf, TransactionContext context, SoaHeader soaHeader, TSoaTransport outputSoaTransport, TSoaServiceProtocol outputProtocol, SoaException e) {
        if (outputProtocol != null) {
            try {
                if (outputBuf.writerIndex() > 0)
                    outputBuf.writerIndex(Integer.BYTES);

                soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
                soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
                outputProtocol.writeMessageBegin(new TMessage(soaHeader.getServiceName() + ":" + soaHeader.getMethodName(), TMessageType.REPLY, context.getSeqid()));
                outputProtocol.writeMessageEnd();

                outputSoaTransport.flush();

                ctx.writeAndFlush(outputBuf);

                LOGGER.info("{} {} {} response header:{} body:{null}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.toString());
            } catch (Throwable e1) {
                LOGGER.error(e1.getMessage(), e1);
            }
        }
    }
}
