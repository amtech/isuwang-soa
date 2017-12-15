package com.isuwang.dapeng.impl.handler;

import com.isuwang.dapeng.api.container.Application;
import com.isuwang.dapeng.api.container.ContainerFactory;
import com.isuwang.dapeng.api.extension.Dispatcher;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.List;
import java.util.Map;

/**
 * Created by lihuimin on 2017/12/7.
 */
public class SoaServerHandler extends ChannelInboundHandlerAdapter {

    private Dispatcher dispatcher;

    private final Boolean useThreadPool = SoaSystemEnvProperties.SOA_CONTAINER_USETHREADPOOL;

    private Map<ProcessorKey, SoaServiceDefinition<?>> processors;

    public SoaServerHandler(){
        List<Application> apps= ContainerFactory.getContainer().getApplications();
        for (Application app : apps) {
            processors.putAll(app.getServiceProcessors());
        }
    }

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
        SoaServiceDefinition processor = processors.get(new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName()));

        ContainerFactory.getContainer().getDispatcher().processRequest(ctx,parser,processor,message);
    }


    private void fillTranscationContex(TransactionContext context) {

    }


}
