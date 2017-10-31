package com.isuwang.soa.serializer;

import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by lihuimin on 2017/10/27.
 */
public class SerializerBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerializerBean.class);

    public <T> ByteBuf serializerStruct(T structBean, TCommonBeanSerializer<T> structSerializer) throws TException {

        final ByteBuf byteBuf = Unpooled.directBuffer(8192);
        final TSoaTransport outputSoaTransport = new TSoaTransport(byteBuf);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputSoaTransport);
        structSerializer.write(structBean, outputProtocol);
        return byteBuf;
    }

    public <T> T deserializerStruct(ByteBuf buff, TCommonBeanSerializer<T> structSerializer) throws TException {

        final TSoaTransport inputSoaTransport = new TSoaTransport(buff);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputSoaTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
