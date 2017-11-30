package com.isuwang.dapeng.remoting.netty;

import com.isuwang.dapeng.core.SoaBaseCode;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.remoting.AsyncRequestWithTimeout;
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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by tangliu on 2016/1/13.
 */
public class SoaClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaClient.class);

    private final String host;
    private final int port;
    private final int readerIdleTimeSeconds = 15;
    private final int writerIdleTimeSeconds = 10;
    private final int allIdleTimeSeconds = 0;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();

    private Bootstrap b = null;
    private Channel channel = null;

    /**
     * 根据请求的序列号（seqid），存储请求的结果，多个线程请求的时候才能返回正确的结果
     */
    private final Map<String, ByteBuf[]> caches = new ConcurrentHashMap<>();

    private static final Map<String, CompletableFuture> futureCaches = new ConcurrentHashMap<>();

    /**
     * 优先队列,根据超时时间排序，最先超时的排在前面（超时时间为请求时间+用户设置的超时时间）
     */
    private static final Queue<AsyncRequestWithTimeout> futuresCachesWithTimeout = new PriorityQueue<>((o1, o2) -> (int) (o1.getTimeout() - o2.getTimeout()));

    public SoaClient(String host, int port) throws SoaException {
        this.host = host;
        this.port = port;

        try {
            connect(host, port);
        } catch (Exception e) {
            throw new SoaException(SoaBaseCode.NotConnected);
        }
//        initBootstrap();
    }

    protected Bootstrap initBootstrap() {
        b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new IdleStateHandler(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds), new SoaDecoder(), new SoaIdleHandler(), new SoaClientHandler(callBack));
            }
        });
        return b;
    }

    /**
     * 创建连接
     *
     * @param host
     * @param port
     * @return
     * @throws Exception
     */
    private synchronized Channel connect(String host, int port) throws Exception {
        if (channel != null && channel.isActive())
            return channel;

        try {
            Bootstrap b = initBootstrap();
            return channel = b.connect(host, port).sync().channel();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new SoaException(SoaBaseCode.NotConnected);
        }
//        return null;
    }


    /**
     * 回调方法，SoaClientHandler收到返回结果，即调用此方法处理返回结果，取出seqid,
     * 将结果放入对应的caches中，并释放锁，使得等待的线程返回结果
     */
    private SoaClientHandler.CallBack callBack = msg -> {
        // length(4) stx(1) version(...) protocol(1) seqid(4) header(...) body(...) etx(1)
        int readerIndex = msg.readerIndex();
        msg.skipBytes(7);
        int seqid = msg.readInt();

        msg.readerIndex(readerIndex);

        ByteBuf[] byteBufs = caches.get(String.valueOf(seqid));

        if (byteBufs == null) {

            if (futureCaches.containsKey(String.valueOf(seqid))) {

                CompletableFuture<ByteBuf> future = (CompletableFuture<ByteBuf>) futureCaches.get(String.valueOf(seqid));
                future.complete(msg);

                futureCaches.remove(String.valueOf(seqid));
            } else {
                LOGGER.error("返回结果超时，siqid为：" + String.valueOf(seqid));
                msg.release();
            }
        } else {
            synchronized (byteBufs) {
                byteBufs[0] = msg;

                byteBufs = caches.get(String.valueOf(seqid));
                // if peer wait    byebuffs != null   if peer out of synchornized, bytebuffs == null
                if(byteBufs != null) {
                    byteBufs.notify();
                }
            }
        }
    };

    /**
     * 发送请求，阻塞等待结果再返回
     *
     * @param seqid
     * @param request
     * @return
     */
    public ByteBuf send(int seqid, ByteBuf request) throws Exception {
        if (channel == null || !channel.isActive())
            connect(host, port);

        //means that this channel is not idle and would not managered by IdleConnectionManager
        IdleConnectionManager.remove(channel);

        ByteBuf[] byteBufs = new ByteBuf[1];

        caches.put(String.valueOf(seqid), byteBufs);

        synchronized (byteBufs) {
            try {
                channel.writeAndFlush(request);

                //等待返回结果，soaClientHandler会将结果写入caches并释放锁，此时返回
                if (byteBufs[0] != null)
                    return byteBufs[0];

                byteBufs.wait(50000);

                return byteBufs[0];
            } finally {
                caches.remove(String.valueOf(seqid));
            }
        }
    }

    /**
     * 发送异步请求
     *
     * @param seqid
     * @param request
     * @return
     */
    public void send(int seqid, ByteBuf request, CompletableFuture<ByteBuf> future, long timeout) throws Exception {

        if (channel == null || !channel.isActive())
            connect(host, port);

        IdleConnectionManager.remove(channel);
        futureCaches.put(String.valueOf(seqid), future);

        AsyncRequestWithTimeout fwt = new AsyncRequestWithTimeout(String.valueOf(seqid), timeout, future);
        futuresCachesWithTimeout.add(fwt);

        channel.writeAndFlush(request);
    }

    /**
     * 定时任务，使得超时的异步任务返回异常给调用者
     */
    private static long DEFAULT_SLEEP_TIME = 1000L;

    static {

        final Thread asyncCheckTimeThread = new Thread("Check Async Timeout Thread") {
            @Override
            public void run() {
                while (true) {
                    try {
                        checkAsyncTimeout();
                    } catch (Exception e) {
                        LOGGER.error("Check Async Timeout Thread Error", e);
                    }
                }
            }
        };
        asyncCheckTimeThread.start();
    }

    private static void checkAsyncTimeout() throws InterruptedException {

        AsyncRequestWithTimeout fwt = futuresCachesWithTimeout.peek();

        while (fwt != null && fwt.getTimeout() < System.currentTimeMillis()) {
            LOGGER.info("异步任务({})超时...", fwt.getSeqid());
            futuresCachesWithTimeout.remove();

            CompletableFuture future = futureCaches.get(fwt.getSeqid());
            future.completeExceptionally(new SoaException(SoaBaseCode.TimeOut));
            futureCaches.remove(fwt.getSeqid());

            fwt = futuresCachesWithTimeout.peek();
        }
        Thread.sleep(DEFAULT_SLEEP_TIME);
    }

    /**
     * 关闭连接
     */
    public void close() {
        if (channel != null)
            channel.close();

        channel = null;
    }

    public void shutdown() {
        if (workerGroup != null)
            workerGroup.shutdownGracefully();
    }

}
