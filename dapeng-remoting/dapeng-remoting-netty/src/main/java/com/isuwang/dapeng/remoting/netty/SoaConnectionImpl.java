package com.isuwang.dapeng.remoting.netty;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.remoting.SoaConnection;
import com.isuwang.org.apache.thrift.TApplicationException;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Deprecated
public class SoaConnectionImpl implements SoaConnection {

    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    private SoaClient soaClient;

    public SoaConnectionImpl(String host, int port) {
        try {
            soaClient = new SoaClient(host, port);
        } catch (SoaException e) {
            LOGGER.error("connect to {}:{} failed", host, port);
        }
    }

    public <REQ, RESP> RESP send(REQ request, RESP response, TBeanSerializer<REQ> requestSerializer, TBeanSerializer<RESP> responseSerializer) throws TException {
        InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();

        final ByteBuf requestBuf = Unpooled.directBuffer(8192);
        final TSoaTransport outputSoaTransport = new TSoaTransport(requestBuf);

        TSoaServiceProtocol outputProtocol;
        ByteBuf responseBuf = null;

        try {
            outputProtocol = new TSoaServiceProtocol(outputSoaTransport, true);
            outputProtocol.writeMessageBegin(new TMessage(soaHeader.getServiceName() + ":" + soaHeader.getMethodName(), TMessageType.CALL, context.getSeqid()));
            requestSerializer.write(request, outputProtocol);
            outputProtocol.writeMessageEnd();

            outputSoaTransport.flush();//在报文头部写入int,代表报文长度(不包括自己)
            if (soaClient == null) {
                throw new SoaException(SoaBaseCode.NotConnected);
            }
            responseBuf = soaClient.send(context.getSeqid(), requestBuf); //发送请求，返回结果

            if (responseBuf == null) {
                throw new SoaException(SoaBaseCode.TimeOut);
//                throw new TException("request time out.");
            } else {
                final TSoaTransport inputSoaTransport = new TSoaTransport(responseBuf);
                TSoaServiceProtocol inputProtocol = new TSoaServiceProtocol(inputSoaTransport, true);

                TMessage msg = inputProtocol.readMessageBegin();
                soaHeader=InvocationContext.Factory.getCurrentInstance().getHeader();
                if (TMessageType.EXCEPTION == msg.type) {
                    TApplicationException x = TApplicationException.read(inputProtocol);
                    inputProtocol.readMessageEnd();
                    throw x;
                } else if (context.getSeqid() != msg.seqid) {
                    throw new TApplicationException(4, soaHeader.getMethodName() + " failed: out of sequence response");
                } else {
                    if ("0000".equals(soaHeader.getRespCode().get())) {
                        responseSerializer.read(response, inputProtocol);
                        inputProtocol.readMessageEnd();
                    } else {
                        throw new SoaException(soaHeader.getRespCode().get(), soaHeader.getRespMessage().get());
                    }

                    return response;
                }
            }
        } catch (SoaException e) {
            LOGGER.error(e.getMessage(), e);
            throw e;

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);

            throw new SoaException(SoaBaseCode.UnKnown);
        } finally {
            outputSoaTransport.close();

            if (requestBuf.refCnt() > 0)
                requestBuf.release();

            // to see SoaDecoder: ByteBuf msg = in.slice(readerIndex, length + Integer.BYTES).retain();
            if (responseBuf != null)
                responseBuf.release();
        }
    }


    /**
     * 异步调用，返回一个Future,等待该future完成或异常
     *
     * @param request
     * @param response
     * @param requestSerializer
     * @param responseSerializer
     * @param <REQ>
     * @param <RESP>
     * @return
     * @throws TException
     */
    public <REQ, RESP> Future<RESP> sendAsync(REQ request, RESP response, TBeanSerializer<REQ> requestSerializer, TBeanSerializer<RESP> responseSerializer, long timeout) throws TException {

        InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        SoaHeader soaHeader = context.getHeader();

        final ByteBuf requestBuf = Unpooled.directBuffer(8192);
        final TSoaTransport outputSoaTransport = new TSoaTransport(requestBuf);

        TSoaServiceProtocol outputProtocol;

        try {
            outputProtocol = new TSoaServiceProtocol(outputSoaTransport, true);
            outputProtocol.writeMessageBegin(new TMessage(soaHeader.getServiceName() + ":" + soaHeader.getMethodName(), TMessageType.CALL, context.getSeqid()));
            requestSerializer.write(request, outputProtocol);
            outputProtocol.writeMessageEnd();

            outputSoaTransport.flush();//在报文头部写入int,代表报文长度(不包括自己)
            if (soaClient == null) {
                throw new SoaException(SoaBaseCode.NotConnected);
            }

            CompletableFuture<ByteBuf> responseBufFuture = new CompletableFuture<>();
            soaClient.send(context.getSeqid(), requestBuf, responseBufFuture, timeout);

            final CompletableFuture<RESP> finalResponseFuture = new CompletableFuture<>();

            responseBufFuture.whenComplete((responseBuf, ex) -> {

                if (responseBuf != null) {

                    final TSoaTransport inputSoaTransport = new TSoaTransport(responseBuf);
                    TSoaServiceProtocol inputProtocol = new TSoaServiceProtocol(inputSoaTransport, true);
                    InvocationContext.Factory.setCurrentInstance(context);

                    try {
                        TMessage msg = inputProtocol.readMessageBegin();
                        SoaHeader resultSoaHeader = InvocationContext.Factory.getCurrentInstance().getHeader();
                        if (TMessageType.EXCEPTION == msg.type) {
                            TApplicationException x = TApplicationException.read(inputProtocol);
                            inputProtocol.readMessageEnd();
                            throw x;
                        } else if (context.getSeqid() != msg.seqid) {
                            throw new TApplicationException(4, resultSoaHeader.getMethodName() + " failed: out of sequence response");
                        } else {
                            if ("0000".equals(resultSoaHeader.getRespCode().get())) {
                                responseSerializer.read(response, inputProtocol);
                                inputProtocol.readMessageEnd();

                                finalResponseFuture.complete(response);
                            } else {
                                throw new SoaException(resultSoaHeader.getRespCode().get(), resultSoaHeader.getRespMessage().get());
                            }
                        }
                    } catch (SoaException e) {
                        LOGGER.error(e.getMessage(), e);
                        finalResponseFuture.completeExceptionally(e);

                    } catch (Throwable e) {
                        LOGGER.error(e.getMessage(), e);
                        finalResponseFuture.completeExceptionally(new SoaException(SoaBaseCode.UnKnown));

                    } finally {
                        if (responseBuf != null)
                            responseBuf.release();

                        if (requestBuf.refCnt() > 0)
                            requestBuf.release();
                    }
                } else {
                    finalResponseFuture.completeExceptionally(ex);
                }
            });

            return finalResponseFuture;

        } catch (Throwable e) {
            LOGGER.error(e.getMessage(), e);
            throw new SoaException(SoaBaseCode.UnKnown);
        } finally {
            outputSoaTransport.close();
        }
    }

}
