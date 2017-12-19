package com.isuwang.dapeng.remoting.netty;

import com.isuwang.dapeng.core.Context;
import com.isuwang.dapeng.core.definition.SoaServiceDefinition;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

/**
 * Created by lihuimin on 2017/12/8.
 */
public interface Dispatcher {

    public void processRequest(ChannelHandlerContext ctx, TProtocol contentProtocol, SoaServiceDefinition processor, ByteBuf message, Context context)throws TException;

}
