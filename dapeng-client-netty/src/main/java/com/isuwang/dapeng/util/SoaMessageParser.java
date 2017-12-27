package com.isuwang.dapeng.util;

import com.isuwang.dapeng.client.netty.TSoaTransport;
import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.Context;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.SoaHeaderSerializer;
import com.isuwang.dapeng.core.enums.CodecProtocol;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TBinaryProtocol;
import com.isuwang.org.apache.thrift.protocol.TCompactProtocol;
import com.isuwang.org.apache.thrift.protocol.TJSONProtocol;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import io.netty.buffer.ByteBuf;

/**
 * Created by lihuimin on 2017/12/22.
 */
public class SoaMessageParser<RESP> {

    public final byte STX = 0x02;
    public final byte ETX = 0x03;
    public final byte VERSION = 1;

    private SoaHeader header;
    private RESP body;
    private BeanSerializer<RESP> bodySerializer;
    private CodecProtocol protocol=CodecProtocol.CompressedBinary;
    private int seqid;

    private ByteBuf buffer;

    public SoaMessageParser(ByteBuf buffer,BeanSerializer<RESP> bodySerializer){
        this.buffer = buffer;
        this.bodySerializer = bodySerializer;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public RESP getBody() {
        return body;
    }

    public SoaMessageParser<RESP> parse() throws TException{
        TSoaTransport transport = new TSoaTransport(buffer);
        TBinaryProtocol headerProtocol = new TBinaryProtocol(transport);

        // length(int32) stx(int8) version(int8) protocol(int8) seqid(i32) header(struct) body(struct) etx(int8)

        byte stx = headerProtocol.readByte();
        if (stx != STX) {// 通讯协议不正确
            throw new TException("通讯协议不正确(起始符)");
        }
        byte version = headerProtocol.readByte();
        if (version!=VERSION) {
            throw new TException("通讯协议不正确(协议版本号)");
        }

        TProtocol bodyProtocol = null;
        CodecProtocol protocol = CodecProtocol.toCodecProtocol(headerProtocol.readByte());
        switch (protocol) {
            case Binary:
                bodyProtocol = new TBinaryProtocol(transport);
                break;
            case CompressedBinary:
                bodyProtocol = new TCompactProtocol(transport);
                break;
            case Json:
                bodyProtocol = new TJSONProtocol(transport);
                break;
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }

        this.protocol = protocol;
        this.seqid = headerProtocol.readI32();
        SoaHeader soaHeader =new SoaHeaderSerializer().read( headerProtocol);
        this.header = soaHeader;
        this.body=bodySerializer.read(bodyProtocol);

        byte etx = headerProtocol.readByte();
        if(etx != ETX){
            throw new TException("通讯协议不正确(结束符)");
        }
        return this;
    }



}
