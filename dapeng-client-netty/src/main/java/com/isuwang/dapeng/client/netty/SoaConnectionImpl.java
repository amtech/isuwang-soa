package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.client.filter.LoadBalanceFilter;
import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.filter.*;
import com.isuwang.dapeng.util.SoaMessageBuilder;
import com.isuwang.dapeng.util.SoaMessageParser;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SoaConnectionImpl implements SoaConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    private final String host;
    private final int port;
    private Channel channel = null;
    private NettyClient client; // Netty Channel
    private AtomicInteger seqidAtomic = new AtomicInteger(0);

    /**
     * 请求的响应. 要不是成功的响应, 要不是异常对象
     *
     * @param <RESP>
     */
    static class Result<RESP> {
        public final RESP success;
        public final SoaException exception;

        Result(RESP success, SoaException exception) {
            this.success = success;
            this.exception = exception;
        }
    }

    public SoaConnectionImpl(String host, int port) {
        this.client = NettyClientFactory.getNettyClient();
        this.host = host;
        this.port = port;
        try {
            channel = connect(host, port);
        } catch (Exception e) {
            LOGGER.error("connect to {}:{} failed", host, port);
        }
    }


    @Override
    public <REQ, RESP> RESP send(
            String service, String version, String method,
            REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws SoaException {

        // InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();

        int seqid = this.seqidAtomic.getAndIncrement();

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }

            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {

                ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                // TODO filter
                checkChannel();
                ByteBuf responseBuf = client.send(channel, seqid, requestBuf); //发送请求，返回结果

                Result<RESP> result = processResponse(responseBuf, responseSerializer);
                ctx.setAttribute("result", result);

                onExit(ctx, getPrevChain(ctx));
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) {

            }
        };

        SharedChain sharedChain = new SharedChain(new LoadBalanceFilter(), new ArrayList<>(), dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        Result<RESP> result = (Result<RESP>) filterContext.getAttribute("result");
        assert (result != null);

        if (result.success != null) {
            return result.success;
        } else {
            throw result.exception;
        }
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer,
                                              BeanSerializer<RESP> responseSerializer, long timeout) throws SoaException {

        int seqid = this.seqidAtomic.getAndIncrement();

        Filter dispatchFilter = new Filter() {
            private FilterChain getPrevChain(FilterContext ctx) {
                SharedChain chain = (SharedChain) ctx.getAttach(this, "chain");
                return new SharedChain(chain.head, chain.shared, chain.tail, chain.size() - 2);
            }

            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                try {

                    ByteBuf requestBuf = buildRequestBuf(service, version, method, seqid, request, requestSerializer);

                    CompletableFuture<ByteBuf> responseBufFuture = null;
                    try {
                        checkChannel();
                        responseBufFuture = client.sendAsync(channel, seqid, requestBuf, timeout); //发送请求，返回结果
                    } catch (Exception e) { // TODO
                        LOGGER.error(e.getMessage(), e);
                        Result<RESP> result = new Result<>(null,
                                new SoaException(SoaCode.UnKnown, "TODO"));
                        ctx.setAttribute("result", result);
                        onExit(ctx, getPrevChain(ctx));
                        return;
                    }

                    responseBufFuture.exceptionally(ex -> {
                        LOGGER.error(ex.getMessage(), ex);
                        Result<RESP> result = new Result<>(null,
                                new SoaException(SoaCode.TimeOut));
                        ctx.setAttribute("result", result);
                        try {
                            onExit(ctx, getPrevChain(ctx));
                        } catch (SoaException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        return null;
                    });

                    responseBufFuture.thenAccept(responseBuf -> {
                        Result<RESP> result = processResponse(responseBuf, responseSerializer);
                        ctx.setAttribute("result", result);
                        try {
                            onExit(ctx, getPrevChain(ctx));
                        } catch (SoaException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error(e.getMessage(), e);
                    // TODO
                    Result<RESP> result = new Result<>(null,
                            new SoaException(SoaCode.UnKnown, "TODO"));
                    ctx.setAttribute("result", result);
                    onExit(ctx, getPrevChain(ctx));
                }
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {
                prev.onExit(ctx);
            }
        };

        Filter headFilter = new Filter() {
            @Override
            public void onEntry(FilterContext ctx, FilterChain next) throws SoaException {
                CompletableFuture<RESP> resultFuture = new CompletableFuture<>();
                ctx.setAttach(this, "future", resultFuture);
                next.onEntry(ctx);
            }

            @Override
            public void onExit(FilterContext ctx, FilterChain prev) throws SoaException {

                CompletableFuture<RESP> future = (CompletableFuture<RESP>) ctx.getAttach(this, "future");
                Result<RESP> result = (Result<RESP>) ctx.getAttribute("result");
                if (result.success != null) {
                    future.complete(result.success);
                } else {
                    future.completeExceptionally(result.exception);
                }
            }
        };
        // Head, LoadBalance, .. dispatch
        SharedChain sharedChain = new SharedChain(headFilter, new ArrayList<>(), dispatchFilter, 0);

        FilterContextImpl filterContext = new FilterContextImpl();
        filterContext.setAttach(dispatchFilter, "chain", sharedChain);

        try {
            sharedChain.onEntry(filterContext);
        } catch (TException e) {
            throw new SoaException(e);
        }
        CompletableFuture<RESP> resultFuture = (CompletableFuture<RESP>) filterContext.getAttach(headFilter, "future");

        assert (resultFuture != null);

        return resultFuture;
    }

    private SoaHeader buildHeader(String service, String version, String method) {
        SoaHeader header = new SoaHeader();
        header.setServiceName(service);
        header.setVersionName(version);
        header.setMethodName(method);

        // TODO fill header from InvocationContext

        return header;
    }

    private <REQ> ByteBuf buildRequestBuf(String service, String version, String method, int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws SoaException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);//Unpooled.directBuffer(8192);  // TODO Pooled

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        // TODO set protocol
        SoaHeader header = buildHeader(service, version, method);
        try {
            ByteBuf buf = builder.buffer(requestBuf)
                    .header(header)
                    .body(request, requestSerializer)
                    .seqid(seqid)
                    .build();
            return buf;
        } catch (TException e) {
            throw new SoaException(e);
        }
    }

    private <RESP> Result<RESP> processResponse(ByteBuf responseBuf, BeanSerializer<RESP> responseSerializer) {
        try {
            if (responseBuf == null) {
                return new Result<>(null, new SoaException(SoaCode.TimeOut));
            } else {
                SoaMessageParser parser = new SoaMessageParser(responseBuf, responseSerializer).parseHeader();
                // TODO fill InvocationContext.lastInfo from response.Header
                SoaHeader respHeader = parser.getHeader();

                if ("0000".equals(respHeader.getRespCode().get())) {
                    parser.parseBody();
                    RESP resp = (RESP) parser.getBody();
                    assert (resp != null);
                    //ctx.setAttribute(filter, "response", resp);
                    return new Result<>(resp, null);
                } else {
                    return new Result<>(null, new SoaException(
                            (respHeader.getRespCode().isPresent()) ? respHeader.getRespCode().get() : SoaCode.UnKnown.getCode(),
                            (respHeader.getRespMessage().isPresent()) ? respHeader.getRespMessage().get() : SoaCode.UnKnown.getMsg()));
                }

            }
        } catch (TException ex) {
            return new Result<>(null,
                    new SoaException(SoaCode.UnKnown, "TODO")); // TODO
        } finally {
            responseBuf.release();
        }

    }

    /**
     * 创建连接
     */
    private synchronized Channel connect(String host, int port) throws SoaException {
        if (channel != null && channel.isActive())
            return channel;

        try {
            return channel = this.client.connect(host, port);
        } catch (Exception e) {
            throw new SoaException(SoaCode.NotConnected);
        }
    }

    private void checkChannel() throws SoaException {
        if (channel == null || !channel.isActive())
            connect(host, port);
    }

}
