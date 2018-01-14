package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.core.filter.FilterChain;
import com.isuwang.dapeng.core.filter.FilterContext;
import com.isuwang.dapeng.core.filter.Filter;
import com.isuwang.dapeng.impl.plugins.netty.SoaMessageProcessor;
import com.isuwang.dapeng.impl.plugins.netty.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeadFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadFilter.class);


    @Override
    public void onEntry(FilterContext ctx, FilterChain next)  {

        try {
            next.onEntry(ctx);
        } catch (TException e) {
            LOGGER.error(e.getMessage(),e);
        }


    }

    @Override
    public void onExit(FilterContext ctx, FilterChain prev)  {
        // 第一个filter不需要调onExit
        ByteBuf outputBuf = null;
        try {
            ChannelHandlerContext channelHandlerContext = (ChannelHandlerContext) ctx.getAttribute( "channelHandlerContext");
            TransactionContext context = (TransactionContext) ctx.getAttribute("context");
            BeanSerializer serializer = (BeanSerializer) ctx.getAttribute("respSerializer");
            Object result = ctx.getAttribute("result");

            if(channelHandlerContext!=null) {
                outputBuf = channelHandlerContext.alloc().buffer(8192);  // TODO 8192?
                TSoaTransport transport = new TSoaTransport(outputBuf);

                SoaMessageProcessor builder = new SoaMessageProcessor(transport);
                builder.writeHeader(context);
                if(serializer != null && result != null) {
                    builder.writeBody(serializer, result);
                }
                builder.writeMessageEnd();
                transport.flush();
                channelHandlerContext.writeAndFlush(outputBuf);
            }
        }catch (Exception e){
            LOGGER.error(e.getMessage(),e);
            if (outputBuf != null && outputBuf.refCnt() > 0 ){
                outputBuf.release();
            }
        }
    }
}
