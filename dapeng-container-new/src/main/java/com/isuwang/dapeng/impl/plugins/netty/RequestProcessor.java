package com.isuwang.dapeng.impl.plugins.netty;


import com.isuwang.dapeng.api.FilterChain;
import com.isuwang.dapeng.api.FilterContext;
import com.isuwang.dapeng.api.HandlerFilter;
import com.isuwang.dapeng.api.SharedChain;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.impl.filters.HandlerFilterContext;
import com.isuwang.dapeng.impl.filters.TimeoutFilter;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


/**
 * Created by lihuimin on 2017/12/8.
 */
public class RequestProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestProcessor.class);


    public static <I,REQ,RESP> void processRequest(ChannelHandlerContext channelHandlerContext, TProtocol contentProtocol, SoaServiceDefinition<I> processor, ByteBuf message,Context context) throws TException {

            SoaHeader soaHeader = context.getHeader();

            SoaFunctionDefinition<I,REQ, RESP> soaFunction = (SoaFunctionDefinition<I,REQ, RESP>)processor.getFunctins().get(soaHeader.getMethodName());
            REQ args = soaFunction.getReqSerializer().read(contentProtocol);
            contentProtocol.readMessageEnd();
            I iface = processor.getIface();

            SharedChain sharedChain = new SharedChain(new TimeoutFilter(),new HandlerFilter[0],null,0);
            HandlerFilter dispatchFilter = new HandlerFilter() {
                @Override
                public void onEntry(FilterContext ctx, FilterChain next) throws TException {
                    if (soaFunction.isAsync()) {
                        CompletableFuture<RESP> future = soaFunction.applyAsync(iface,args);
                            future.whenComplete((realResult, ex) -> {
                                try {

                                    if (realResult != null) {
                                        process(channelHandlerContext,soaFunction,iface,args,context,realResult,message,true);
                                    } else {
                                        future.completeExceptionally(ex);
                                    }
                                    onExit(ctx,new SharedChain(sharedChain.getHead(),sharedChain.getShared(),this,sharedChain.getCurrentIndex()));
                                } catch (TException e) {
                                    e.printStackTrace();
                                }
                            });
                    } else {
                        RESP result =(RESP) soaFunction.apply(iface, args);
                        process(channelHandlerContext,soaFunction,iface,args,context,result,message,false);
                    }
                }
                @Override
                public void onExit(FilterContext ctx, FilterChain prev) throws TException {

                }
            };
            sharedChain.setTail(dispatchFilter);
            HandlerFilterContext filterContext = new HandlerFilterContext();
            sharedChain.onEntry(filterContext);

    }

    private static void dump(ByteBuf buffer) {
        int readerIndex = buffer.readerIndex();
        int availabe = buffer.readableBytes();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        // XX XX XX XX XX XX XX XX  XX XX XX XX XX XX XX XX  ASCII....
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

    private static <I,REQ,RESP> void process(ChannelHandlerContext channelHandlerContext, SoaFunctionDefinition soaFunction,I iface,REQ args,Context context,RESP result ,ByteBuf message,boolean isAsync) throws TException{
        TSoaTransport transport =null;
        try {
            SoaHeader header = context.getHeader();
            header.setRespCode(Optional.of("0000"));
            header.setRespMessage(Optional.of("ok"));

            ByteBuf byteBuf = channelHandlerContext.alloc().buffer(8192);
            transport = new TSoaTransport(byteBuf);

            SoaMessageProcessor builder = new SoaMessageProcessor(false, transport);
            builder.buildResponse(context);
            soaFunction.getRespSerializer().write(result, new TCompactProtocol(transport));
            builder.writeMessageEnd();

            transport.flush();
            channelHandlerContext.writeAndFlush(byteBuf);
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
        }finally{
            if (message.refCnt() > 0) {
                message.release();
            }
            if(transport!=null){
                transport.close();
            }
            TransactionContext.Factory.removeCurrentInstance();
        }
    }

}
