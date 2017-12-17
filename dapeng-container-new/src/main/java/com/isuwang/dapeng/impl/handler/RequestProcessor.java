package com.isuwang.dapeng.impl.handler;

import com.isuwang.dapeng.api.container.ContainerFactory;
import com.isuwang.dapeng.api.filters.FilterChain;
import com.isuwang.dapeng.api.filters.FilterContext;
import com.isuwang.dapeng.api.filters.HandlerFilter;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.impl.filters.*;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Created by lihuimin on 2017/12/8.
 */
public class RequestProcessor {

    public static SharedChain sharedChain;


    public static <I,REQ,RESP> void processRequest(ChannelHandlerContext ctx, SoaMessageProcessor parser, SoaServiceDefinition<I> processor, ByteBuf message) throws TException {
        try {

            ByteBuf byteBuf = ctx.alloc().buffer(8192);
            TSoaTransport transport = new TSoaTransport(byteBuf);
            SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
            builder.parseSoaMessage();

            TransactionContext context = TransactionContext.Factory.getCurrentInstance();
            SoaHeader soaHeader = context.getHeader();

            SoaFunctionDefinition<I,REQ, RESP> soaFunction = (SoaFunctionDefinition<I,REQ, RESP>)processor.getFunctins().get(soaHeader.getMethodName());
            REQ args = soaFunction.getReqSerializer().read(parser.getContentProtocol());
            parser.getContentProtocol().readMessageEnd();

            SharedChain sharedChain = ContainerFactory.getContainer().getSharedChain();
            HandlerFilter dispatchFilter = new HandlerFilter() {
                @Override
                public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                    if (soaFunction.isAsync()) {
                        final CompletableFuture<Context> futureResult = new CompletableFuture<>();
                        if (soaHeader.isAsyncCall()) {
                            CompletableFuture<Object> future = (CompletableFuture<Object>) ctx.getAttach(this, "response");
                            future.whenComplete((realResult, ex) -> {
                                try {
                                    if (realResult != null) {
                                        AsyncAccept(context, soaFunction, realResult, builder.getContentProtocol(), futureResult);
                                    } else {
                                        TransactionContext.Factory.setCurrentInstance(context);
                                        futureResult.completeExceptionally(ex);
                                    }
                                    onExit(ctx,new SharedChain(null,sharedChain.getShared(),this,sharedChain.getCurrentIndex()-1));
                                } catch (TException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    } else {
                        RESP result = null;
                        result =  soaFunction.apply(processor.getIface(), args);
                        context.getHeader().setRespCode(Optional.of("0000"));
                        context.getHeader().setRespMessage(Optional.of("成功"));
                        builder.buildResponse();
                        soaFunction.getRespSerializer().write(result, builder.getContentProtocol());
                        builder.getContentProtocol().writeMessageEnd();

                        ctx.setAttach(this, "response", result);
                    }
                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) throws TException {

                }
            };

            sharedChain.setTail(dispatchFilter);
            HandlerFilterContext filterContext = new HandlerFilterContext();
            sharedChain.onEntry(filterContext);
            ctx.writeAndFlush(byteBuf);
        } finally{
            if (message.refCnt() > 0) {
                message.release();
            }
        }

    }

    private static void AsyncAccept(Context context, SoaFunctionDefinition soaFunction, Object result, TProtocol out, CompletableFuture future) {

        try {
            TransactionContext.Factory.setCurrentInstance((TransactionContext) context);
            SoaHeader soaHeader = context.getHeader();

            soaHeader.setRespCode(Optional.of("0000"));
            soaHeader.setRespMessage(Optional.of("成功"));
            out.writeMessageBegin(new TMessage(soaHeader.getMethodName(), TMessageType.CALL, context.getSeqid()));
            soaFunction.getRespSerializer().write(result, out);
            out.writeMessageEnd();
            /**
             * 通知外层handler处理结果
             */
            future.complete(context);
        } catch (TException e) {
            e.printStackTrace();
        }

    }

}
