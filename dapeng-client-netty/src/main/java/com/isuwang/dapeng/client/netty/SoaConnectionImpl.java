package com.isuwang.dapeng.client.netty;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.SoaConnection;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.util.SoaMessageBuilder;
import com.isuwang.dapeng.util.SoaMessageParser;
import com.isuwang.org.apache.thrift.TException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class SoaConnectionImpl implements SoaConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);


    private NettyClient client;
    private AtomicInteger seqid = new AtomicInteger(0);

    public SoaConnectionImpl(String host, int port) {
        try {
            client = new NettyClient(host, port);
        } catch (SoaException e) {
            LOGGER.error("connect to {}:{} failed", host, port);
        }
    }


    @Override
    public <REQ, RESP> RESP send(
            String service, String version, String method,
            REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws Exception {

        // InvocationContext context = InvocationContextImpl.Factory.getCurrentInstance();
        int seqid = this.seqid.getAndIncrement();
        ByteBuf requestBuf = buildRequestBuf(service,version,method,seqid,request,requestSerializer);

        // TODO filter
        ByteBuf responseBuf = client.send(seqid, requestBuf); //发送请求，返回结果
        SoaMessageParser parser = new SoaMessageParser(responseBuf, responseSerializer).parse();

        // TODO fill InvocationContext.lastInfo from response.Header
        SoaHeader respHeader = parser.getHeader();

        RESP resp = (RESP)parser.getBody();
        responseBuf.release();

        return resp;
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(String service, String version, String method, REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer, long timeout) throws Exception {

        int seqid = this.seqid.getAndIncrement();
        ByteBuf requestBuf = buildRequestBuf(service,version,method,seqid,request,requestSerializer);

        CompletableFuture<ByteBuf> responseBufFuture = new CompletableFuture<>();
        client.sendAsync(seqid, requestBuf,responseBufFuture,timeout); //发送请求，返回结果

        return responseBufFuture.thenApply((ByteBuf responseBuf) ->{
            try {
                SoaMessageParser parser = new SoaMessageParser(responseBuf, responseSerializer).parse();
                // TODO fill InvocationContext.lastInfo from response.Header
                SoaHeader respHeader = parser.getHeader();
                RESP result = (RESP)parser.getBody();
                responseBuf.release();
                return result;
            } catch (TException e) {
                e.printStackTrace();
                return null;
            }
        });
    }

    private SoaHeader buildHeader(String service, String version, String method) {
        SoaHeader header = new SoaHeader();
        header.setServiceName(service);
        header.setVersionName(version);
        header.setMethodName(method);

        // TODO fill header from InvocationContext

        return header;
    }

    private <REQ> ByteBuf buildRequestBuf(String service, String version, String method,int seqid, REQ request, BeanSerializer<REQ> requestSerializer) throws TException {
        final ByteBuf requestBuf = PooledByteBufAllocator.DEFAULT.buffer(8192);//Unpooled.directBuffer(8192);  // TODO Pooled
        final TSoaTransport reqSoaTransport = new TSoaTransport(requestBuf);

        SoaMessageBuilder<REQ> builder = new SoaMessageBuilder<>();

        // TODO set protocol
        SoaHeader header = buildHeader(service, version, method);
        ByteBuf buf = builder.buffer(requestBuf)
                .header(header)
                .body(request, requestSerializer)
                .seqid(seqid)
                .build();
        return buf;
    }
}
