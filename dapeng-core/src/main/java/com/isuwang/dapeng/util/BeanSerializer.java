package com.isuwang.dapeng.util;

import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;

/**
 * Created by lihuimin on 2017/10/27.
 */
public class BeanSerializer {

    public static <T> byte[] serialize(T structBean, TCommonBeanSerializer<T> structSerializer) throws TException {

        byte [] byteBuf = new byte[8192];
        final TSoaTransport outputSoaTransport = new TSoaTransport(byteBuf, TSoaTransport.Type.Write,0);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputSoaTransport);
        structSerializer.write(structBean, outputProtocol);
        return byteBuf;
    }

    public static <T> T deserialize(byte[] buff, TCommonBeanSerializer<T> structSerializer) throws TException {

        final TSoaTransport inputSoaTransport = new TSoaTransport(buff, TSoaTransport.Type.Read,0);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputSoaTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
