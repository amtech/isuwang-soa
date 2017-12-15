package com.isuwang.dapeng.api.extension;

import com.isuwang.dapeng.impl.handler.SoaMessageProcessor;
import com.isuwang.dapeng.impl.handler.SoaServiceDefinition;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by lihuimin on 2017/12/8.
 */
public interface Dispatcher {

    public void processRequest(ChannelHandlerContext ctx, SoaMessageProcessor parser, SoaServiceDefinition processor, ByteBuf message)throws TException;

}
