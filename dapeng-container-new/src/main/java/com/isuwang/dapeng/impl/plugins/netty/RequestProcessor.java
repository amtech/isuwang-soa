package com.isuwang.dapeng.impl.plugins.netty;


import com.isuwang.dapeng.api.*;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.impl.filters.*;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Created by lihuimin on 2017/12/8.
 */
public class RequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProcessor.class);


    public static <I,REQ,RESP> void processRequest(ChannelHandlerContext channelHandlerContext, TProtocol contentProtocol, SoaServiceDefinition<I> processor, ByteBuf message,Context context) throws TException {
        try {

            SoaHeader soaHeader = context.getHeader();

            SoaFunctionDefinition<I,REQ, RESP> soaFunction = (SoaFunctionDefinition<I,REQ, RESP>)processor.getFunctins().get(soaHeader.getMethodName());
            REQ args = soaFunction.getReqSerializer().read(contentProtocol);
            contentProtocol.readMessageEnd();

            SharedChain sharedChain = new SharedChain(new TimeoutFilter(),new HandlerFilter[0],null,0);
            HandlerFilter dispatchFilter = new HandlerFilter() {
                @Override
                public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                    if (soaFunction.isAsync()) {
                        final CompletableFuture<Context> futureResult = new CompletableFuture<>();
                            CompletableFuture<Object> future = (CompletableFuture<Object>) ctx.getAttach(this, "response");
                            future.whenComplete((realResult, ex) -> {
                                try {
                                    ByteBuf byteBuf = channelHandlerContext.alloc().buffer(8192);
                                    TSoaTransport transport = new TSoaTransport(byteBuf);
                                    SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
                                    builder.buildResponse(context);
                                    if (realResult != null) {
                                        AsyncAccept(context, soaFunction, realResult, builder.getContentProtocol(), futureResult);
                                    } else {
                                        futureResult.completeExceptionally(ex);
                                    }
                                    channelHandlerContext.writeAndFlush(byteBuf);
                                    onExit(ctx,new SharedChain(null,sharedChain.getShared(),this,sharedChain.getCurrentIndex()-1));
                                } catch (TException e) {
                                    e.printStackTrace();
                                }
                            });
                    } else {
                        ByteBuf byteBuf = channelHandlerContext.alloc().buffer(8192);
                        TSoaTransport transport = new TSoaTransport(byteBuf);
                        SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
                        RESP result = null;
                        result =  soaFunction.apply(processor.getIface(), args);
                        context.getHeader().setRespCode(Optional.of("0000"));
                        context.getHeader().setRespMessage(Optional.of("成功"));
                        builder.buildResponse(context);
                        soaFunction.getRespSerializer().write(result, new TCompactProtocol(transport));
                        builder.writeMessageEnd();
                        transport.flush();
                        channelHandlerContext.writeAndFlush(byteBuf);
                    }
                }

                @Override
                public void onExit(FilterContext ctx, FilterChain prev) throws TException {

                }
            };

            sharedChain.setTail(dispatchFilter);
            HandlerFilterContext filterContext = new HandlerFilterContext();
            sharedChain.onEntry(filterContext);
        } finally{
            if (message.refCnt() > 0) {
                message.release();
            }
        }

    }

    private static void dump(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int availabe = buffer.readableBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        System.out.println("=======[");
        int i = 0;
        for(; i<availabe; i++){
            byte b = buffer.readByte();
            baos.write(b);

            String it = String.format("%02x ", b & 0xFF);
            System.out.print( it );

            if(i % 16 == 15) {
               byte[] array = baos.toByteArray();
               System.out.print(' ');
               for(int j=0; j<array.length; j++) {
                   char ch = (char)array[j];
                   if(ch>=0x20 && ch < 0x7F) System.out.print(ch);
                   else System.out.print('.');
               }
               baos = new ByteArrayOutputStream();
               System.out.println();
            }
        }
//        if(baos.size() > 0) {
//
//        }
        System.out.println("]======");

        buffer.readerIndex(readerIndex);
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
