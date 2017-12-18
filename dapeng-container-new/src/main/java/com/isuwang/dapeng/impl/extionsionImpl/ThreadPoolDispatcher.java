package com.isuwang.dapeng.impl.extionsionImpl;

import com.isuwang.dapeng.core.SoaServiceDefinition;
import com.isuwang.dapeng.core.SoaSystemEnvProperties;
import com.isuwang.dapeng.impl.plugins.netty.RequestProcessor;
import com.isuwang.dapeng.remoting.netty.Dispatcher;
import com.isuwang.dapeng.remoting.netty.SoaMessageProcessor;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by lihuimin on 2017/12/8.
 */
public class ThreadPoolDispatcher implements Dispatcher {
    static class ServerThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        ServerThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "trans-pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }


    private volatile static ExecutorService executorService = Executors.newFixedThreadPool(SoaSystemEnvProperties.SOA_CORE_POOL_SIZE, new ServerThreadFactory());


    //统一同步和异步的处理
    @Override
    public void processRequest(ChannelHandlerContext ctx, SoaMessageProcessor parser, SoaServiceDefinition processor, ByteBuf message){

        executorService.execute(() -> {
            try {
                RequestProcessor.processRequest(ctx, parser, processor,message);
            } catch (TException e) {
                e.printStackTrace();
            }
        });

    }
}
