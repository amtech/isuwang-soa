package com.isuwang.dapeng.remoting.socket;

import com.isuwang.dapeng.core.*;
import com.isuwang.dapeng.core.socket.TSoaTransport;
import com.isuwang.dapeng.remoting.SoaCommonConnection;
import com.isuwang.org.apache.thrift.TApplicationException;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TMessage;
import com.isuwang.org.apache.thrift.protocol.TMessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.net.Socket;
import java.util.concurrent.Future;

/**
 * SOA连接
 *
 * @author craneding
 * @date 15/8/5
 */
public class SoaConnectionImpl implements SoaCommonConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoaConnectionImpl.class);

    public <REQ, RESP> RESP send(REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws TException {
        final InvocationContext context = InvocationContext.Factory.getCurrentInstance();
        SoaHeader header = context.getHeader();
        RESP response=null;

        try (
                TSoaTransport transport = new TSoaTransport();
                Socket socket = new Socket(context.getCalleeIp(), context.getCalleePort());
                BufferedOutputStream output = new BufferedOutputStream(socket.getOutputStream());
                BufferedInputStream input = new BufferedInputStream(socket.getInputStream());
        ) {
            socket.setKeepAlive(true);
            socket.setSoTimeout((int) context.getCalleeTimeout());

            transport.setInputStream(input);
            transport.setOutputStream(output);

            TSoaServiceProtocol protocol = new TSoaServiceProtocol(transport, true);

            protocol.writeMessageBegin(new TMessage(header.getServiceName() + ":" + header.getMethodName(), TMessageType.CALL, context.getSeqid()));
            requestSerializer.write(request, protocol);
            protocol.writeMessageEnd();

            transport.flush();
            TMessage msg = protocol.readMessageBegin();
            header =  InvocationContext.Factory.getCurrentInstance().getHeader();
            if (msg.type == TMessageType.EXCEPTION) {
                TApplicationException x = TApplicationException.read(protocol);
                protocol.readMessageEnd();
                throw x;
            } else if (msg.seqid != context.getSeqid()) {
                throw new TApplicationException(4, header.getMethodName() + " failed: out of sequence response");
            } else {
                if ("0000".equals(header.getRespCode().get())) {
                    response=responseSerializer.read(protocol);
                    protocol.readMessageEnd();
                } else {
                    throw new SoaException(header.getRespCode().get(), header.getRespMessage().get());
                }
                return response;
            }
        } catch (TException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);

            throw new TException(e);
        }
    }

    @Override
    public <REQ, RESP> Future<RESP> sendAsync(REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer, long timeout) throws TException {
        return null;
    }

}
