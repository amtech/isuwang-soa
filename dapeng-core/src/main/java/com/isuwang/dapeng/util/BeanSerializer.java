package com.isuwang.dapeng.util;

import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.dapeng.core.TSoaTransport;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Created by lihuimin on 2017/10/27.
 */
public class BeanSerializer {

    public static <T> ByteBuf serialize(T structBean, TCommonBeanSerializer<T> structSerializer) throws TException {

        final ByteBuf byteBuf = Unpooled.directBuffer(8192);
        final TSoaTransport outputSoaTransport = new TSoaTransport(byteBuf);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputSoaTransport);
        structSerializer.write(structBean, outputProtocol);
        return byteBuf;
    }

    public static <T> T deserialize(ByteBuf buff, TCommonBeanSerializer<T> structSerializer) throws TException {

        final TSoaTransport inputSoaTransport = new TSoaTransport(buff);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputSoaTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
