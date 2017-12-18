package com.isuwang.dapeng.impl.plugins.netty;

import com.isuwang.dapeng.core.Context;
import com.isuwang.dapeng.core.SoaServiceDefinition;
import com.isuwang.dapeng.impl.plugins.netty.RequestProcessor;
import com.isuwang.dapeng.remoting.netty.Dispatcher;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class ThreadDispatcher implements Dispatcher {

    @Override
    public void processRequest(ChannelHandlerContext ctx, TProtocol contentProtocol, SoaServiceDefinition processor, ByteBuf message, Context context) throws TException {
       RequestProcessor.processRequest(ctx,contentProtocol,processor,message,context);
    }
}
