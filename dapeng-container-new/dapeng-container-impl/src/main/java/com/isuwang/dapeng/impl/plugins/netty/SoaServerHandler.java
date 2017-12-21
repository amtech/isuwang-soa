package com.isuwang.dapeng.impl.plugins.netty;

import com.isuwang.dapeng.api.*;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.definition.SoaFunctionDefinition;
import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
import com.isuwang.dapeng.impl.filters.HandlerFilterContext;
import com.isuwang.dapeng.impl.filters.TimeoutFilter;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Created by lihuimin on 2017/12/7.
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;
    private final Container container;

    public SoaServerHandler(Container container){
        this.container = container;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        ByteBuf reqMessage = (ByteBuf) msg;
        TSoaTransport inputSoaTransport = new TSoaTransport(reqMessage);
        SoaMessageProcessor parser = new SoaMessageProcessor(false, inputSoaTransport);
        SoaHeader soaHeader = null;

        try {
            soaHeader = parser.parseSoaMessage();

            // parser.service, version, method, header, bodyProtocol
            // TODO explict construct TransactionContext
            TransactionContext context = TransactionContext.Factory.getCurrentInstance();
            context.setHeader(soaHeader);
            SoaServiceDefinition processor = ContainerFactory.getContainer().getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            container.getDispatcher().execute(() -> {
                try {
                    processRequest(ctx, parser.getContentProtocol(), processor, reqMessage, context);
                } catch (TException e) {
                    // TODO
                    e.printStackTrace();
                }
            });
        }
        catch(TException ex){
            // TODO
            ex.printStackTrace();
        }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }

    private <I, REQ, RESP> void processRequest(ChannelHandlerContext channelHandlerContext, TProtocol contentProtocol, SoaServiceDefinition<I> serviceDef,
                                               ByteBuf reqMessage, TransactionContext context) throws TException {

        SoaHeader soaHeader = context.getHeader();
        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));

        SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());
        REQ args = soaFunction.reqSerializer.read(contentProtocol);
        contentProtocol.readMessageEnd();
        reqMessage.release();

        if(reqMessage.refCnt() > 0){
            // TODO
            reqMessage.release();
        }

        //
        I iface = serviceDef.iface;
        //log request
        application.info(this.getClass(),"{} {} {} operatorId:{} operatorName:{} request body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.reqSerializer.toString(args)));

        Filter dispatchFilter = new Filter() {

            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size()-2);
            }
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                if (serviceDef.isAsync) {
                    SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;
                    CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);
                    future.whenComplete((realResult, ex) -> {
                        // TODO refact as same as sync
                        try {
                            if (realResult != null) {
                                //log result
                                application.info(this.getClass(),"{} {} {} operatorId:{} operatorName:{} response body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.respSerializer.toString(realResult)));
                                processResult(channelHandlerContext, soaFunction.respSerializer, context, realResult);
                            } else {
                                future.completeExceptionally(ex);
                            }
                            onExit(ctx, getPrevChain(ctx));
                        } catch (Exception e) {
                            LOGGER.error(e.getMessage(), e);
                            writeErrorMessage(channelHandlerContext,context,soaHeader,new SoaException(SoaBaseCode.UnKnown, e.getMessage()));
                        }
                    });
                } else {
                    try {
                        SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
                        RESP result = (RESP) syncFunction.apply(iface, args);
                        //log result
                        application.info( this.getClass(),"{} {} {} operatorId:{} operatorName:{} response body:{}",soaHeader.getServiceName(),soaHeader.getVersionName(),soaHeader.getMethodName(),soaHeader.getOperatorId(),soaHeader.getOperatorName(),formatToString(soaFunction.respSerializer.toString(result)));
                        processResult(channelHandlerContext, soaFunction.respSerializer, context, result);
                        onExit(ctx, getPrevChain(ctx));
                    } catch (TException e) {
                        LOGGER.error(e.getMessage(), e);
                        writeErrorMessage(channelHandlerContext, context,soaHeader,new SoaException(SoaBaseCode.UnKnown, e.getMessage()));
                    }
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws TException {
                prev.onExit(ctx);
            }
        };
        SharedChain sharedChain = new SharedChain(new TimeoutFilter(), new ArrayList<>(), dispatchFilter, 0);

//        sharedChain.setTail(dispatchFilter);
        HandlerFilterContext filterContext = new HandlerFilterContext();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        sharedChain.onEntry(filterContext);

    }

    private <RESP> void processResult(ChannelHandlerContext channelHandlerContext, TCommonBeanSerializer<RESP> respSerializer, TransactionContext context, RESP result) throws TException {
        try {
            SoaHeader header = context.getHeader();
            header.setRespCode(Optional.of("0000"));
            header.setRespMessage(Optional.of("ok"));

            ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);  // TODO 8192?
            TSoaTransport transport = new TSoaTransport(outputBuf);

            SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
            builder.writeHeader(context);
            // builder.write(result, respSerializer) TODO
            respSerializer.write(result, new TCompactProtocol(transport));
            builder.writeMessageEnd();

            transport.flush();
            channelHandlerContext.writeAndFlush(outputBuf);
        }  finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    private  void writeErrorMessage(ChannelHandlerContext ctx, Context context, SoaHeader soaHeader,  SoaException e) {
        ByteBuf outputBuf = ctx.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
        try {
            soaHeader.setRespCode(Optional.ofNullable(e.getCode()));
            soaHeader.setRespMessage(Optional.ofNullable(e.getMsg()));
            builder.writeHeader(context);
            builder.writeMessageEnd();

            transport.flush();

            ctx.writeAndFlush(outputBuf);

            LOGGER.info("{} {} {} response header:{} body:{null}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.toString());
        } catch (Throwable e1) {
            LOGGER.error(e1.getMessage(), e1);
        }

    }

    private  String formatToString(String msg) {
        if (msg == null)
            return msg;

        msg = msg.indexOf("\r\n") != -1 ? msg.replaceAll("\r\n", "") : msg;

        int len = msg.length();
        int max_len = 128;

        if (len > max_len)
            msg = msg.substring(0, 128) + "...(" + len + ")";

        return msg;
    }

}
