package com.isuwang.dapeng.remoting.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by tangliu on 2016/1/13.
 */
public class SoaClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaClientHandler.class);

    private CallBack callBack;

    public static interface CallBack {
        void onSuccess(ByteBuf msg);
    }

    public SoaClientHandler(CallBack callBack) {
        this.callBack = callBack;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (callBack != null)
            callBack.onSuccess((ByteBuf) msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error(cause.getMessage(), cause);

        ctx.close();
    }
}
