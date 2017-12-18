package com.isuwang.dapeng.impl.handler;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.container.ContainerFactory;
import com.isuwang.dapeng.remoting.netty.Dispatcher;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.dapeng.remoting.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Created by lihuimin on 2017/12/7.
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private Dispatcher dispatcher;

    private final Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        readRequestHeader(ctx, (ByteBuf) msg);
    }


    void readRequestHeader(ChannelHandlerContext ctx, ByteBuf message) throws TException {

        TSoaTransport soaTransport = new TSoaTransport(message);
        SoaMessageProcessor parser = new SoaMessageProcessor(false,soaTransport);
        parser.parseSoaMessage();
        // parser.service, version, method, header, bodyProtocol

        TransactionContext context = TransactionContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();
        fillTranscationContex(context);

        //APlugin.markRequestBegin(); // container.registerFilter(...); container.startThread(...);
        SoaServiceDefinition processor = ContainerFactory.getContainer().getServiceProcessors().get(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));

        //TODO: 需要从netty 提供接口
       // ContainerFactory.getContainer().getDispatcher().processRequest(ctx,parser,processor,message);
    }


    private void fillTranscationContex(TransactionContext context) {

    }


}
