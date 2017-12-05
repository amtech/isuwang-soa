package com.isuwang.dapeng.core;

import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;
import com.isuwang.org.apache.thrift.transport.TTransport;

import java.nio.ByteBuffer;

/**
 * TSoa服务协议
 *
 * @author craneding
 * @date 15/8/5
 */
public class TSoaServiceProtocol extends TProtocol {

    private final byte STX = 0x02;
    private final byte ETX = 0x03;
    private final String VERSION = "1.0.0";

    private TProtocol realHeaderProtocol;
    private TProtocol realContentProtocol;

    private final boolean isRequestFlag;

    //private TCommonTransport trans;

    public TSoaServiceProtocol(TTransport trans, boolean isRequestFlag) {
        super(trans);
        //this.trans = trans;
        this.isRequestFlag = isRequestFlag;
    }

    @Override
    public void writeMessageBegin(TMessage message) throws TException {
        final Context context = isRequestFlag ? InvocationContext.Factory.getCurrentInstance() : TransactionContext.Factory.getCurrentInstance();

        if (realHeaderProtocol == null) {
            realHeaderProtocol = new TBinaryProtocol(getTransport());
        }

        // length(int32) stx(int8) version(string) protocol(int8) seqid(int32) header(struct) body(struct) etx(int8)

        realHeaderProtocol.writeByte(STX);
        realHeaderProtocol.writeString(VERSION);
        realHeaderProtocol.writeByte(context.getCodecProtocol().getCode());
        realHeaderProtocol.writeI32(context.getSeqid());

        switch (context.getCodecProtocol()) {
            case Binary:
                realContentProtocol = new TBinaryProtocol(getTransport());
                break;
            case CompressedBinary:
                realContentProtocol = new TCompactProtocol(getTransport());
                break;
            case Json:
                realContentProtocol = new TJSONProtocol(getTransport());
                break;
            case Xml:
                realContentProtocol = null;
                break;
        }

        new SoaHeaderSerializer().write(context.getHeader(), this);

        realContentProtocol.writeMessageBegin(message);
    }

    @Override
    public void writeMessageEnd() throws TException {
        realContentProtocol.writeMessageEnd();

        realHeaderProtocol.writeByte(ETX);
    }

    @Override
    public void writeStructBegin(TStruct struct) throws TException {
        realContentProtocol.writeStructBegin(struct);
    }

    @Override
    public void writeStructEnd() throws TException {
        realContentProtocol.writeStructEnd();
    }

    @Override
    public void writeFieldBegin(TField field) throws TException {
        realContentProtocol.writeFieldBegin(field);
    }

    @Override
    public void writeFieldEnd() throws TException {
        realContentProtocol.writeFieldEnd();
    }

    @Override
    public void writeFieldStop() throws TException {
        realContentProtocol.writeFieldStop();
    }

    @Override
    public void writeMapBegin(TMap map) throws TException {
        realContentProtocol.writeMapBegin(map);
    }

    @Override
    public void writeMapEnd() throws TException {
        realContentProtocol.writeMapEnd();
    }

    @Override
    public void writeListBegin(TList list) throws TException {
        realContentProtocol.writeListBegin(list);
    }

    @Override
    public void writeListEnd() throws TException {
        realContentProtocol.writeListEnd();
    }

    @Override
    public void writeSetBegin(TSet set) throws TException {
        realContentProtocol.writeSetBegin(set);
    }

    @Override
    public void writeSetEnd() throws TException {
        realContentProtocol.writeSetEnd();
    }

    @Override
    public void writeBool(boolean b) throws TException {
        realContentProtocol.writeBool(b);
    }

    @Override
    public void writeByte(byte b) throws TException {
        realContentProtocol.writeByte(b);
    }

    @Override
    public void writeI16(short i16) throws TException {
        realContentProtocol.writeI16(i16);
    }

    @Override
    public void writeI32(int i32) throws TException {
        realContentProtocol.writeI32(i32);
    }

    @Override
    public void writeI64(long i64) throws TException {
        realContentProtocol.writeI64(i64);
    }

    @Override
    public void writeDouble(double dub) throws TException {
        realContentProtocol.writeDouble(dub);
    }

    @Override
    public void writeString(String str) throws TException {
        realContentProtocol.writeString(str);
    }

    @Override
    public void writeBinary(ByteBuffer buf) throws TException {
        realContentProtocol.writeBinary(buf);
    }

    @Override
    public TMessage readMessageBegin() throws TException {
        final Context context = isRequestFlag ? InvocationContext.Factory.getCurrentInstance() : TransactionContext.Factory.getCurrentInstance();

        if (realHeaderProtocol == null) {
            realHeaderProtocol = new TBinaryProtocol(getTransport());
        }

        // length(int32) stx(int8) version(string) protocol(int8) header(struct) body(struct) etx(int8)

        byte stx = realHeaderProtocol.readByte();
        if (stx != STX) {// 通讯协议不正确
            throw new TException("通讯协议不正确(起始符)");
        }

        // version
        try {
            String version = realHeaderProtocol.readString();
            if (!VERSION.equals(version)) {
                throw new TException("通讯协议不正确(协议版本号)");
            }
        }catch (Exception e){
            throw new TException("版本不兼容" ,new TVersionException());
        }

        byte protocol = realHeaderProtocol.readByte();
        context.setCodecProtocol(Context.CodecProtocol.toCodecProtocol(protocol));
        switch (context.getCodecProtocol()) {
            case Binary:
                realContentProtocol = new TBinaryProtocol(getTransport());
                break;
            case CompressedBinary:
                realContentProtocol = new TCompactProtocol(getTransport());
                break;
            case Json:
                realContentProtocol = new TJSONProtocol(getTransport());
                break;
            case Xml:
                //realContentProtocol = null;
                throw new TException("通讯协议不正确(包体协议)");
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }

        context.setSeqid(realHeaderProtocol.readI32());

        SoaHeader soaHeader =new SoaHeaderSerializer().read( this);
        context.setHeader(soaHeader);

        return realContentProtocol.readMessageBegin();
    }

    public SoaHeader readHeader(byte codecProtocol) throws TException{
        final Context context = isRequestFlag ? InvocationContext.Factory.getCurrentInstance() : TransactionContext.Factory.getCurrentInstance();
        context.setCodecProtocol(Context.CodecProtocol.toCodecProtocol(codecProtocol));
        switch (context.getCodecProtocol()) {
            case Binary:
                realContentProtocol = new TBinaryProtocol(getTransport());
                break;
            case CompressedBinary:
                realContentProtocol = new TCompactProtocol(getTransport());
                break;
            case Json:
                realContentProtocol = new TJSONProtocol(getTransport());
                break;
            case Xml:
                //realContentProtocol = null;
                throw new TException("通讯协议不正确(包体协议)");
            default:
                throw new TException("通讯协议不正确(包体协议)");
        }
        SoaHeader soaHeader =new SoaHeaderSerializer().read( this);
        return soaHeader;
    }

    @Override
    public void readMessageEnd() throws TException {
        realContentProtocol.readMessageEnd();

        byte etx = realHeaderProtocol.readByte();
        if (etx != ETX) {// 通讯协议不正确
            throw new TException("通讯协议不正确(结束符)");
        }
    }

    @Override
    public TStruct readStructBegin() throws TException {
        return realContentProtocol.readStructBegin();
    }

    @Override
    public void readStructEnd() throws TException {
        realContentProtocol.readStructEnd();
    }

    @Override
    public TField readFieldBegin() throws TException {
        return realContentProtocol.readFieldBegin();
    }

    @Override
    public void readFieldEnd() throws TException {
        realContentProtocol.readFieldEnd();
    }

    @Override
    public TMap readMapBegin() throws TException {
        return realContentProtocol.readMapBegin();
    }

    @Override
    public void readMapEnd() throws TException {
        realContentProtocol.readMapEnd();
    }

    @Override
    public TList readListBegin() throws TException {
        return realContentProtocol.readListBegin();
    }

    @Override
    public void readListEnd() throws TException {
        realContentProtocol.readListEnd();
    }

    @Override
    public TSet readSetBegin() throws TException {
        return realContentProtocol.readSetBegin();
    }

    @Override
    public void readSetEnd() throws TException {
        realContentProtocol.readSetEnd();
    }

    @Override
    public boolean readBool() throws TException {
        return realContentProtocol.readBool();
    }

    @Override
    public byte readByte() throws TException {
        return realContentProtocol.readByte();
    }

    @Override
    public short readI16() throws TException {
        return realContentProtocol.readI16();
    }

    @Override
    public int readI32() throws TException {
        return realContentProtocol.readI32();
    }

    @Override
    public long readI64() throws TException {
        return realContentProtocol.readI64();
    }

    @Override
    public double readDouble() throws TException {
        return realContentProtocol.readDouble();
    }

    @Override
    public String readString() throws TException {
        return realContentProtocol.readString();
    }

    @Override
    public ByteBuffer readBinary() throws TException {
        return realContentProtocol.readBinary();
    }
}
