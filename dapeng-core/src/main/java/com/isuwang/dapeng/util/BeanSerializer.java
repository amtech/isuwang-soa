package com.isuwang.dapeng.util;

import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;

/**
 * Created by lihuimin on 2017/10/27.
 */
public class BeanSerializer {

    public static <T> byte[] serialize(T structBean, com.isuwang.dapeng.core.BeanSerializer<T> structSerializer) throws TException {

        byte [] byteBuf = new byte[8192];
        final TCommonTransport outputCommonTransport = new TCommonTransport(byteBuf, TCommonTransport.Type.Write);

        TCompactProtocol outputProtocol = new TCompactProtocol(outputCommonTransport);
        structSerializer.write(structBean, outputProtocol);
        return outputCommonTransport.getByteBuf();
    }

    public static <T> T deserialize(byte[] buff, com.isuwang.dapeng.core.BeanSerializer<T> structSerializer) throws TException {

        final TCommonTransport inputCommonTransport = new TCommonTransport(buff, TCommonTransport.Type.Read);

        TCompactProtocol intputProtocol = new TCompactProtocol(inputCommonTransport);
        T struct = structSerializer.read(intputProtocol);
        return struct;
    }




}
