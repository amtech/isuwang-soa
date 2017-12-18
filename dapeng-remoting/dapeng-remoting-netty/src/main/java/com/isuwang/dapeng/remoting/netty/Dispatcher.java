package com.isuwang.dapeng.remoting.netty;

import com.isuwang.dapeng.core.SoaServiceDefinition;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by lihuimin on 2017/12/8.
 */
public interface Dispatcher {

    public void processRequest(ChannelHandlerContext ctx, SoaMessageProcessor parser, SoaServiceDefinition processor, ByteBuf message)throws TException;

}
