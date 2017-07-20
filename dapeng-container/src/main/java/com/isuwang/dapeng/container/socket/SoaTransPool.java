package com.isuwang.dapeng.container.socket;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.socket.TSoaTransport;
import com.isuwang.org.apache.thrift.TProcessor;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Soa 交易线程池
 *
 * @author craneding
 * @date 15/9/17
 */
public class SoaTransPool {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaTransPool.class);

    private final int corePoolSize = 100;
    private final int maximumPoolSize = 200;
    private final long keepAliveTime = 60;
    private final TimeUnit unit = TimeUnit.SECONDS;
    private final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(50);
    private final ThreadFactory threadFactory = new DefaultThreadFactory("soa-trans");

    private final ExecutorService threadPool;

    public SoaTransPool() {
        threadPool = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public void execute(SoaCodecTask command) {
        threadPool.execute(command);
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public static class SoaCodecTask implements Runnable {
        private Socket client;
        private Map<ProcessorKey, TProcessor<?>> soaProcessors;

        public SoaCodecTask(Socket client, Map<ProcessorKey, TProcessor<?>> soaProcessors) {
            this.client = client;
            this.soaProcessors = soaProcessors;
        }

        @Override
        public void run() {
            final TransactionContext context = TransactionContext.Factory.getCurrentInstance();
            final SoaHeader soaHeader = new SoaHeader();
            final TSoaTransport soaTransport = new TSoaTransport();

            context.setHeader(soaHeader);

            TSoaServiceProtocol protocol = null;
            BufferedInputStream input = null;
            BufferedOutputStream out = null;

            try {
                input = new BufferedInputStream(client.getInputStream());
                out = new BufferedOutputStream(client.getOutputStream());

                soaTransport.setInputStream(input);
                soaTransport.setOutputStream(out);

                protocol = new TSoaServiceProtocol(soaTransport, false);
                TMessage tMessage = protocol.readMessageBegin();

                context.setSeqid(tMessage.seqid);

                TProcessor<?> soaProcessor = soaProcessors.get(new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName()));
                if (soaProcessor == null) {
                    throw new SoaException(SoaBaseCode.NotFoundServer);
                }

                soaProcessor.process(protocol, protocol);

                soaTransport.flush();
            } catch (IOException e) {
                LOGGER.error(e.getMessage(), e);
            } catch (SoaException e) {
                LOGGER.error(e.getMessage(), e);

                if (protocol != null) {
                    try {
                        soaHeader.setRespCode(Optional.of(e.getCode()));
                        soaHeader.setRespMessage(Optional.of(e.getMsg()));
                        protocol.writeMessageBegin(new TMessage(soaHeader.getServiceName() + ":" + soaHeader.getMethodName(), TMessageType.REPLY, context.getSeqid()));
                        protocol.writeMessageEnd();

                        soaTransport.flush();
                    } catch (Exception e1) {
                        LOGGER.error(e1.getMessage(), e1);
                    }
                }
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            } finally {
                if (input != null)
                    try {
                        input.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                if (out != null)
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                    }

                if (soaTransport != null)
                    soaTransport.close();
            }
        }
    }

    public class DefaultThreadFactory implements ThreadFactory {
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String name;

        public DefaultThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, name + "-" + threadNumber.getAndIncrement());
        }
    }
}
