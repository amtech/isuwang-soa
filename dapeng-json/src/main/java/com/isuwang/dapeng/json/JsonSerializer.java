package com.isuwang.dapeng.json;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.enums.CodecProtocol;
import com.isuwang.dapeng.core.metadata.*;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Stack;

public class JsonSerializer implements BeanSerializer<String> {

    private final Struct struct;
    private final ByteBuf byteBuf;

    public JsonSerializer(Struct struct, ByteBuf byteBuf) {
        this.struct = struct;
        this.byteBuf = byteBuf;
    }

    // thrift -> json
    private void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();

        while (true) {
            TField field = iproto.readFieldBegin();
            if (field.type == TType.STOP)
                break;

            Field fld = struct.getFields().get(0); // TODO get fld by field.id
            boolean skip = fld == null;

            switch (field.type) {
                case TType.VOID:
                    break;
                case TType.BOOL:
                    boolean value = iproto.readBool();
                    if (!skip) {
                        writer.onBoolean(value);
                    }
                    break;
                case TType.BYTE:
                    // TODO
                case TType.DOUBLE:
                    // TODO
                case TType.I16:
                    // TODO
                case TType.I32:
                    // TODO:
                case TType.I64:
                    // TODO:
                case TType.STRING:
                    // TODO:
                case TType.STRUCT:
                    if (!skip) {
                        String subStructName = fld.dataType.qualifiedName;
                        Struct subStruct = null;    // findStruct(subStructName)
                        new JsonSerializer(subStruct, byteBuf).read(iproto, writer);
                    } else {
                        // skip contents
                    }
                    break;
                case TType.MAP:
                case TType.SET:
                case TType.LIST:
                default:

            }
        }


        iproto.readStructEnd();
    }

    @Override
    public String read(TProtocol iproto) throws TException {

        JsonWriter writer = new JsonWriter();
        read(iproto, writer);
        return writer.toString();
    }

    /**
     * format:
     * url:http://xxx/api/callService?serviceName=xxx&version=xx&method=xx
     * post body:
     * {
     * "orderType":2,
     * "supplyers":[209,304,211]
     * }
     * <p>
     * InvocationContext and SoaHeader should be ready before
     */
    static class Json2ThriftCallback implements JsonCallback {

        private final TProtocol oproto;
        private final Method method;
        private final Service service;
        private final ByteBuf byteBuf;
        private CodecProtocol protocol;

        class StackNode {
            final DataType dataType;
            final int byteBufPosition;
            int size = 0;

            StackNode(DataType dataType, int byteBufPosition) {
                this.dataType = dataType;
                this.byteBufPosition = byteBufPosition;
            }
        }

        // current struct
        StackNode current;

        /**
         * @param oproto
         * @param service
         * @param method
         * @param byteBuf
         */
        public Json2ThriftCallback(TProtocol oproto, Service service, Method method, ByteBuf byteBuf) {
            this.oproto = oproto;
            this.method = method;
            this.service = service;
            this.byteBuf = byteBuf;
        }


        /*  {  a:, b:, c: { ... }, d: [ { }, { } ]  }
         *
         *  init                -- [], topStruct
         *  onStartObject
         *    onStartField a    -- [topStruct], DataType a
         *    onEndField a
         *    ...
         *    onStartField c    -- [topStruct], StructC
         *      onStartObject
         *          onStartField
         *          onEndField
         *          ...
         *      onEndObject     -- [], topStruct
         *    onEndField c
         *
         *    onStartField d
         *      onStartArray    -- [topStruct] structD
         *          onStartObject
         *          onEndObject
         *      onEndArray      -- []
         *    onEndField d
         */
        Stack<StackNode> history = new Stack<>();

        @Override
        public void onStartObject() throws TException {
            if (current == null) {
                oproto.writeStructBegin(new TStruct(method.name));
            } else {
                switch (current.dataType.kind) {
                    case STRUCT:
//                        oproto.writeStructBegin();
                        break;
                }
            }
        }

        @Override
        public void onEndObject() throws TException {
            oproto.writeStructEnd();
        }

        @Override
        public void onStartArray() throws TException {
            assert current.dataType.kind == DataType.KIND.LIST;
            stackNew(new StackNode(current.dataType.valueType, byteBuf.writerIndex()));
            oproto.writeListBegin(new TList(dataType2Byte(current.dataType.valueType), 0));
        }

        @Override
        public void onEndArray() throws TException {
            StackNode lastNode = pop();
            int beginPosition = lastNode.byteBufPosition;
            int arraySize = lastNode.size;

            int currentIndex = byteBuf.writerIndex();

            byteBuf.writerIndex(beginPosition);
            oproto.writeListBegin(new TList(dataType2Byte(lastNode.dataType), arraySize));
            byteBuf.writerIndex(currentIndex);

            oproto.writeListEnd();
        }

        @Override
        public void onStartField(String name) throws TException {
            // if field is Struct, stackNew(subStruct)
            // if field is List

            Field field = findField(name, method.request);
            oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
            stackNew(field.dataType);
        }

        @Override
        public void onEndField() throws TException {
            pop();
            oproto.writeFieldEnd();
        }

        @Override
        public void onBoolean(boolean value) throws TException {
            assert current.kind == DataType.KIND.BOOLEAN;
            oproto.writeBool(value);
        }

        @Override
        public void onNumber(double value) throws TException {
            switch (current.kind) {
                case SHORT:
                    oproto.writeI16((short) value);
                    break;
                case INTEGER:
                    oproto.writeI32((int) value);
                    break;
                case LONG:
                    oproto.writeI64((long) value);
                    break;
                case DOUBLE:
                    oproto.writeDouble(value);
                    break;
                case BYTE:
                    //TODO
                    break;
                default:
                    throw new TException("DataType(" + current.kind + ") for " + current.qualifiedName + " is not a Number");

            }
        }

        @Override
        public void onNull() throws TException {
            //TODO
            throw new TException("DataType(" + current.kind + ") for " + current.qualifiedName + " is a null");
        }

        @Override
        public void onString(String value) throws TException {
            assert current.kind == DataType.KIND.STRING;
            oproto.writeString(value);
        }

        private void stackNew(StackNode node) {
            history.push(this.current);
            this.current = node;
        }

        private StackNode pop() {
            return history.pop();
        }

        private Field findField(String fieldName, Struct struct) {
            List<Field> fields = struct.getFields();

            for (Field field : fields) {
                if (field.getName().equals(fieldName)) {
                    return field;
                }
            }

            return null;
        }


        private byte dataType2Byte(DataType type) {
            switch (type.kind) {
                case BOOLEAN:
                    return TType.BOOL;

                case BYTE:
                    return TType.BYTE;

                case DOUBLE:
                    return TType.DOUBLE;

                case SHORT:
                    return TType.I16;

                case INTEGER:
                    return TType.I32;

                case LONG:
                    return TType.I64;

                case STRING:
                    return TType.STRING;

                case STRUCT:
                    return TType.STRUCT;

                case MAP:
                    return TType.MAP;

                case SET:
                    return TType.SET;

                case LIST:
                    return TType.LIST;

                case ENUM:
                    return TType.I32;

                case VOID:
                    return TType.VOID;

                case DATE:
                    return TType.I64;

                case BIGDECIMAL:
                    return TType.STRING;

                case BINARY:
                    return TType.STRING;

                default:
                    break;
            }

            return TType.STOP;
        }
    }

    // json -> thrift
    @Override
    public void write(String input, TProtocol oproto) throws TException {


        new JsonParser(input, new Json2ThriftCallback(oproto, null, null, byteBuf));

    }

    @Override
    public void validate(String s) throws TException {

    }

    @Override
    public String toString(String s) {
        return s;
    }


}
