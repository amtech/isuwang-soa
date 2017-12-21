//package com.isuwang.dapeng.impl.plugins.netty;
//
//
//import com.isuwang.dapeng.api.*;
//import com.isuwang.dapeng.core.*;
//import com.isuwang.dapeng.core.definition.SoaFunctionDefinition;
//import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
//import com.isuwang.dapeng.impl.filters.HandlerFilterContext;
//import com.isuwang.dapeng.impl.filters.TimeoutFilter;
//import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
//import com.isuwang.dapeng.remoting.netty.TSoaTransport;
//import com.isuwang.org.apache.thrift.TException;
//import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
//import com.isuwang.org.apache.thrift.protocol.TProtocol;
//import io.netty.buffer.ByteBuf;
//import io.netty.channel.ChannelHandlerContext;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.ByteArrayOutputStream;
//import java.util.ArrayList;
//import java.util.Map;
//import java.util.Optional;
//import java.util.concurrent.CompletableFuture;
//
//
///**
// * Created by lihuimin on 2017/12/8.
// */
//public class RequestProcessor {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProcessor.class);
//
//    public static <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext, TProtocol contentProtocol, SoaServiceDefinition<I> serviceDef, ByteBuf message, Context context) throws TException {
//
//        SoaHeader soaHeader = context.getHeader();
//        Application application = ContainerFactory.getContainer().getApplication(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));
//
//        SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());
//        REQ args = soaFunction.reqSerializer.read(contentProtocol);
//        contentProtocol.readMessageEnd();
//        I iface = serviceDef.iface;
//        //log request
//        application.info(RequestProcessor.class,"{} {} {} operatorId:{} operatorName:{} request body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.reqSerializer.toString(args)));
//
//        Filter dispatchFilter = new Filter() {
//
//            private FilterChain getPrevChain(FilterContext ctx) {
//                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
//                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size()-2);
//            }
//            @Override
//            public void onEntry(FilterContext ctx, FilterChain next) throws TException {
//                if (serviceDef.isAsync) {
//                    SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;
//                    CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);
//                    future.whenComplete((realResult, ex) -> {
//                        // TODO refact as same as sync
//                        try {
//                            if (realResult != null) {
//                                //log result
//                                application.info(RequestProcessor.class,"{} {} {} operatorId:{} operatorName:{} response body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.respSerializer.toString(realResult)));
//                                processResult(channelHandlerContext, soaFunction.respSerializer, context, realResult, message);
//                            } else {
//                                future.completeExceptionally(ex);
//                            }
//                            onExit(ctx, getPrevChain(ctx));
//                        } catch (Exception e) {
//                            LOGGER.error(e.getMessage(), e);
//                            writeErrorMessage(channelHandlerContext,context,soaHeader,new SoaException(SoaBaseCode.UnKnown, e.getMessage()));
//                        }
//                    });
//                } else {
//                    try {
//                        SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
//                        RESP result = (RESP) syncFunction.apply(iface, args);
//                        //log result
//                        application.info(RequestProcessor.class,"{} {} {} operatorId:{} operatorName:{} response body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.respSerializer.toString(result)));
//                        processResult(channelHandlerContext, soaFunction.respSerializer, context, result, message);
//                        onExit(ctx, getPrevChain(ctx));
//                    } catch (TException e) {
//                        LOGGER.error(e.getMessage(), e);
//                        writeErrorMessage(channelHandlerContext, context,soaHeader,new SoaException(SoaBaseCode.UnKnown, e.getMessage()));
//                    }
//                }
//            }
//
//            @Override
//            public void onExit(FilterContext ctx, FilterChain prev) throws TException {
//                prev.onExit(ctx);
//            }
//        };
//        SharedChain sharedChain = new SharedChain(new TimeoutFilter(), new ArrayList<>(), dispatchFilter, 0);
//
////        sharedChain.setTail(dispatchFilter);
//        HandlerFilterContext filterContext = new HandlerFilterContext();
//        filterContext.setAttach(dispatchFilter, "chain", sharedChain);
//
//        sharedChain.onEntry(filterContext);
//
//    }
//
//    private static <RESP> void processResult(ChannelHandlerContext channelHandlerContext, TCommonBeanSerializer<RESP> respSerializer, Context context, RESP result, ByteBuf message) throws TException {
//        TSoaTransport transport = null;
//        try {
//            SoaHeader header = context.getHeader();
//            header.setRespCode(Optional.of("0000"));
//            header.setRespMessage(Optional.of("ok"));
//
//            ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);
//            transport = new TSoaTransport(outputBuf);
//
//            SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
//            builder.writeHeader(context);
//            respSerializer.write(result, new TCompactProtocol(transport));
//            builder.writeMessageEnd();
//
//            transport.flush();
//            channelHandlerContext.writeAndFlush(outputBuf);
//        } catch (TException e) {
//            throw e;
//        } finally {
//            if (message.refCnt() > 0) {
//                message.release();
//            }
//            if (transport != null) {
//                transport.close();
//            }
//            TransactionContext.Factory.removeCurrentInstance();
//        }
//    }
//
//    private static void writeErrorMessage(ChannelHandlerContext ctx, Context context, SoaHeader soaHeader,  SoaException e) {
//        ByteBuf outputBuf = ctx.alloc().buffer(8192);
//        TSoaTransport transport = new TSoaTransport(outputBuf);
//        SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
//        try {
//            soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
//            soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
//            builder.writeHeader(context);
//            builder.writeMessageEnd();
//
//            transport.flush();
//
//            ctx.writeAndFlush(outputBuf);
//
//            LOGGER.info("{} {} {} response header:{} body:{null}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.toString());
//        } catch (Throwable e1) {
//            LOGGER.error(e1.getMessage(), e1);
//        }
//
//    }
//
//    private static String formatToString(String msg) {
//        if (msg == null)
//            return msg;
//
//        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;
//
//        int len = msg.length();
//        int max_len = 128;
//
//        if (len > max_len)
//            msg = msg.substring(0, 128) + "...(" + len + ")";
//
//        return msg;
//    }
//
//}
