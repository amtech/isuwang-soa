package com.isuwang.dapeng.impl.plugins.netty;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.api.ContainerFactory;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        readRequestHeader(ctx, (ByteBuf) msg);
    }


    void readRequestHeader(ChannelHandlerContext ctx, ByteBuf message) throws TException {

        TSoaTransport soaTransport = new TSoaTransport(message);
        SoaMessageProcessor parser = new SoaMessageProcessor(false,soaTransport);
        SoaHeader soaHeader =parser.parseSoaMessage();
        // parser.service, version, method, header, bodyProtocol

        Context context = TransactionContext.Factory.getCurrentInstance();
        //APlugin.markRequestBegin(); // container.registerFilter(...); container.startThread(...);
        SoaServiceDefinition processor = ContainerFactory.getContainer().getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));

        //TODO: 需要从netty 提供接口
        //ContainerFactory.getContainer().getDispatcher().processRequest(ctx,parser.getContentProtocol(),processor,message,context);
        new ThreadPoolDispatcher().processRequest(ctx,parser.getContentProtocol(),processor,message,context);
    }



}
