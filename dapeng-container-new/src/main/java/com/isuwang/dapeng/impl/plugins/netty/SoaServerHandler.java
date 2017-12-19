package com.isuwang.dapeng.impl.plugins.netty;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.api.ContainerFactory;
import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lihuimin on 2017/12/7.
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaServerHandler.class);

    private final Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        readRequestHeader(ctx, (ByteBuf) msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }

    void readRequestHeader(ChannelHandlerContext ctx, ByteBuf message) {

        TSoaTransport inputSoaTransport = new TSoaTransport(message);
        SoaMessageProcessor parser = new SoaMessageProcessor(false, inputSoaTransport);
        SoaHeader soaHeader = null;
        try {
            soaHeader = parser.parseSoaMessage();

            // parser.service, version, method, header, bodyProtocol

            Context context = TransactionContext.Factory.getCurrentInstance();
            SoaServiceDefinition processor = ContainerFactory.getContainer().getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));

            if (useThreadPool) {
                RequestProcessor.processRequest(ctx, parser.getContentProtocol(), processor, message, context);
            } else {
                new ThreadPoolDispatcher().processRequest(ctx, parser.getContentProtocol(), processor, message, context);
            }

        } catch (Exception e) {
            LOGGER.error(e.getMessage(),e);
        }finally {
            inputSoaTransport.close();
        }
//        Executor executor = null;
//        executor.execute( ()-> {
//            try {
//                RequestProcessor.processRequest(ctx, parser.getContentProtocol(), processor, message, context);
//            } catch (TException e) {
//                // TODO
//                e.printStackTrace();
//            }
//        });

//        ContainerFactory.getContainer().getDispatcher().processRequest(ctx,parser.getContentProtocol(),processor,message,context);
    }


}
