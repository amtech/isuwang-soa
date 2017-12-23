package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.SoaBaseCode;
import com.isuwang.dapeng.core.SoaException;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by lihuimin on 2017/12/21.
 */
public class NettyClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClient.class);


    private final String host;
    private final int port;

    private final int readerIdleTimeSeconds = 15;
    private final int writerIdleTimeSeconds = 10;
    private final int allIdleTimeSeconds = 0;

    private Bootstrap bootstrap = null;
    private Channel channel = null;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private static final Map<Integer, CompletableFuture> futureCaches = new ConcurrentHashMap<>();

    public NettyClient(String host, int port) throws SoaException {
        this.host = host;
        this.port = port;
        try {
            connect(host, port);
        } catch (Exception e) {
            throw new SoaException(SoaBaseCode.NotConnected);
        }
   }

    /**
     * 创建连接
     *
     */
    private synchronized Channel connect(String host, int port) throws Exception {
        if (channel != null && channel.isActive())
            return channel;

        try {
            Bootstrap b = initBootstrap();
            return channel = b.connect(host, port).sync().channel();
        } catch (Exception e) {
            throw new SoaException(SoaBaseCode.NotConnected);
        }
    }

    protected Bootstrap initBootstrap() {
        bootstrap = new Bootstrap();
        bootstrap.group(workerGroup);
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds), new SoaDecoder(), new SoaIdleHandler(),new SoaClientHandler(callBack));
            }
        });
        return bootstrap;
    }

    public ByteBuf send(int seqid, ByteBuf request) throws Exception {
        checkChannel();

        //means that this channel is not idle and would not managered by IdleConnectionManager
        IdleConnectionManager.remove(channel);

        CompletableFuture<ByteBuf> future = new CompletableFuture<ByteBuf>();

        futureCaches.put(seqid, future);

        try {
            channel.writeAndFlush(request);
            ByteBuf respByteBuf = future.get(1000, TimeUnit.MILLISECONDS);
            return respByteBuf;
        } finally {
            futureCaches.remove(seqid);
        }

    }

    public void sendAsync(int seqid, ByteBuf request, CompletableFuture<ByteBuf> future, long timeout) throws Exception {

        checkChannel();

        IdleConnectionManager.remove(channel);
        futureCaches.put(seqid, future);
        channel.writeAndFlush(request);
    }

    private SoaClientHandler.CallBack callBack = msg -> {
        // length(4) stx(1) version(...) protocol(1) seqid(4) header(...) body(...) etx(1)
        int readerIndex = msg.readerIndex();
        msg.skipBytes(7);
        int seqid = msg.readInt();

        msg.readerIndex(readerIndex);

        if (futureCaches.containsKey(seqid)) {
            CompletableFuture<ByteBuf> future = (CompletableFuture<ByteBuf>) futureCaches.get(seqid);
            future.complete(msg);
        }else{

            LOGGER.error("返回结果超时，siqid为：" + String.valueOf(seqid));
            msg.release();
        }
    };

    private void checkChannel() throws Exception {
        if (channel == null || !channel.isActive())
            connect(host, port);
    }

}
