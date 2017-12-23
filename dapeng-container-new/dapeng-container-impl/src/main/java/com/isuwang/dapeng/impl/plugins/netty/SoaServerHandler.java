package com.isuwang.dapeng.impl.plugins.netty;


import com.isuwang.dapeng.api.*;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.definition.SoaFunctionDefinition;
import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
import com.isuwang.dapeng.impl.filters.HandlerFilterContext;
import com.isuwang.dapeng.impl.filters.TimeoutFilter;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Created by lihuimin on 2017/12/7.
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Container container;

    public SoaServerHandler(Container container) {
        this.container = container;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf reqMessage = (ByteBuf) msg;
        TSoaTransport inputSoaTransport = new TSoaTransport(reqMessage);
        SoaMessageProcessor parser = new SoaMessageProcessor(inputSoaTransport);
        SoaHeader soaHeader = null;

        try {
            final TransactionContext context = TransactionContext.Factory.createNewInstance();
            // parser.service, version, method, header, bodyProtocol
            soaHeader = parser.parseSoaMessage(context);

            SoaServiceDefinition processor = container.getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            container.getDispatcher().execute(() -> {
                try {
                    processRequest(ctx, parser.getContentProtocol(), processor, reqMessage, context);
                } catch (TException e) {
                    // TODO
                    e.printStackTrace();
                }
            });
        } catch (TException ex) {
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
        Application application = container.getApplication(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

        SoaFunctionDefinition<I, REQ, RESP> soaFunction = (SoaFunctionDefinition<I, REQ, RESP>) serviceDef.functions.get(soaHeader.getMethodName());
        REQ args = soaFunction.reqSerializer.read(contentProtocol);
        contentProtocol.readMessageEnd();
        reqMessage.release();

        while (reqMessage.refCnt() > 0) {
            application.error(this.getClass(), "request ByteBuf did not release correctly.The current refCnt is " + reqMessage.refCnt(), new Throwable());
            reqMessage.release();
        }

        //
        I iface = serviceDef.iface;
        //log request
        application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} request body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), formatToString(soaFunction.reqSerializer.toString(args)));

        Filter dispatchFilter = new Filter() {

            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }

            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                if (serviceDef.isAsync) {
                    SoaFunctionDefinition.Async asyncFunc = (SoaFunctionDefinition.Async) soaFunction;
                    CompletableFuture<RESP> future = (CompletableFuture<RESP>) asyncFunc.apply(iface, args);
                    future.whenComplete((realResult, ex) -> {
                        processResult(channelHandlerContext, soaFunction, context, realResult, application);
                        onExit(ctx, getPrevChain(ctx));
                    });
                } else {
                    SoaFunctionDefinition.Sync syncFunction = (SoaFunctionDefinition.Sync) soaFunction;
                    RESP result = (RESP) syncFunction.apply(iface, args);
                    processResult(channelHandlerContext, soaFunction, context, result, application);
                    onExit(ctx, getPrevChain(ctx));
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev){
                try {
                    prev.onExit(ctx);
                } catch (TException e) {
                    e.printStackTrace();  //TODO
                }
            }
        };
        SharedChain sharedChain = new SharedChain(new TimeoutFilter(), container.getFilters(), dispatchFilter, 0);

        HandlerFilterContext filterContext = new HandlerFilterContext();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        sharedChain.onEntry(filterContext);

    }

    private <RESP> void processResult(ChannelHandlerContext channelHandlerContext, SoaFunctionDefinition soaFunction, TransactionContext context, Object result, Application application) {
        SoaHeader soaHeader = context.getHeader();
        soaHeader.setRespCode(Optional.of("0000"));
        soaHeader.setRespMessage(Optional.of("ok"));
        try {
            application.info(this.getClass(), "{} {} {} operatorId:{} operatorName:{} response body:{}", soaHeader.getServiceName(), soaHeader.getVersionName(), soaHeader.getMethodName(), soaHeader.getOperatorId(), soaHeader.getOperatorName(), formatToString(soaFunction.respSerializer.toString(result)));

            ByteBuf outputBuf = channelHandlerContext.alloc().buffer(8192);  // TODO 8192?
            TSoaTransport transport = new TSoaTransport(outputBuf);

            SoaMessageProcessor builder = new SoaMessageProcessor(transport);
            builder.writeHeader(context);
            builder.writeBody(soaFunction.respSerializer, result);
            builder.writeMessageEnd();

            transport.flush();
            channelHandlerContext.writeAndFlush(outputBuf);
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            writeErrorMessage(channelHandlerContext, context, soaHeader, new SoaException(SoaBaseCode.UnKnown, e.getMessage()));
        } finally {
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

    private void writeErrorMessage(ChannelHandlerContext ctx, TransactionContext context, SoaHeader soaHeader, SoaException e) {
        ByteBuf outputBuf = ctx.alloc().buffer(8192);
        TSoaTransport transport = new TSoaTransport(outputBuf);
        SoaMessageProcessor builder = new SoaMessageProcessor(transport);
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

    private String formatToString(String msg) {
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
