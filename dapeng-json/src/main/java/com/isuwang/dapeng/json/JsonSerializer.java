package com.isuwang.dapeng.json;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.metadata.*;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class JsonSerializer implements BeanSerializer<String> {

    private final Struct respStruct;
    private final ByteBuf byteBuf;
    private final Service service;
    private final Method method;

    public JsonSerializer(Struct respStruct, ByteBuf byteBuf, Service service, Method method) {
        this.respStruct = respStruct;
        this.byteBuf = byteBuf;
        this.service = service;
        this.method = method;
    }

    // thrift -> json
    private void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();
        writer.onStartObject();

        while (true) {
            TField field = iproto.readFieldBegin();
            if (field.type == TType.STOP)
                break;

            List<Field> flds = respStruct.getFields().stream().filter(_field->{return _field.tag == field.id;}).collect(Collectors.toList()); // TODO get fld by field.id

            Field fld = flds.isEmpty()?null:flds.get(0);

            boolean skip = fld == null;

            switch (field.type) {
                case TType.VOID:
                    break;
                case TType.BOOL:
                    boolean boolValue = iproto.readBool();
                    if (!skip) {
                        writer.onStartField(fld.name);
                        writer.onBoolean(boolValue);
                        writer.onEndField();
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
                    String strValue = iproto.readString();
                    if (!skip) {
                        writer.onStartField(fld.name);
                        writer.onString(strValue);
                        writer.onEndField();
                    }
                    break;
                case TType.STRUCT:
                    if (!skip) {
                        String subStructName = fld.dataType.qualifiedName;
                        Struct subStruct = findStruct(subStructName, service);
                        writer.onStartField(subStructName);
                        new JsonSerializer(subStruct, byteBuf, service, method).read(iproto, writer);
                        writer.onEndField();
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
        writer.onEndObject();
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
    class Json2ThriftCallback implements JsonCallback {

        private final TProtocol oproto;

        class StackNode {
            final DataType dataType;
            /**
             * byteBuf position when this node created
             */
            final int byteBufPosition;

            final Struct struct;
            /**
             * if dataType is a Collection(such as LIST, MAP, SET etc), elSize represents the size of the Collection.
             */
            private int elCount = 0;

            StackNode(final DataType dataType, final int byteBufPosition, final Struct struct) {
                this.dataType = dataType;
                this.byteBufPosition = byteBufPosition;
                this.struct = struct;
            }

            void increaseElement() {
                elCount++;
            }

            int getElCount() {
                return elCount;
            }
        }

        // current struct
        StackNode current;
        boolean inited = false;

        /**
         * @param oproto
         */
        public Json2ThriftCallback(TProtocol oproto) {
            this.oproto = oproto;
            DataType initDataType = new DataType();
            initDataType.setKind(DataType.KIND.STRUCT);
            initDataType.qualifiedName = method.request.name;
            this.current = new StackNode(initDataType, byteBuf.writerIndex(), method.request);
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
            assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;
            //TODO 多重struct的处理
            //TODO MAP的处理, key, value的类型
            if (!inited) {
                oproto.writeStructBegin(new TStruct(current.struct.name));
                inited = true;
            } else {
                switch (current.dataType.kind) {
                    case STRUCT:
                        Struct struct = current.struct;//findStruct(current.dataType.qualifiedName, service);
                        if (struct == null) {
                            //TODO
                        }
                        oproto.writeStructBegin(new TStruct(struct.name));
                        break;
                    case MAP:
                        oproto.writeMapBegin(new TMap(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), 0));
                        break;
                }
            }
        }

        @Override
        public void onEndObject() throws TException {
            assert current.dataType.kind == DataType.KIND.STRUCT || current.dataType.kind == DataType.KIND.MAP;

            switch (current.dataType.kind) {
                case STRUCT:
                    oproto.writeFieldStop();
                    oproto.writeStructEnd();
                    break;
                case MAP:
                    oproto.writeMapEnd();

                    reWriteByteBuf();
                    break;
            }
        }

        /**
         * 由于目前拿不到集合的元素个数, 暂时设置为0个
         *
         * @throws TException
         */
        @Override
        public void onStartArray() throws TException {
            assert isCollectionKind(current.dataType.kind);

            switch (current.dataType.kind) {
                case LIST:
                    oproto.writeListBegin(new TList(dataType2Byte(current.dataType.valueType), 0));
                    break;
                case SET:
                    oproto.writeSetBegin(new TSet(dataType2Byte(current.dataType.valueType), 0));
                    break;
            }

            if (isCollectionKind(current.dataType.valueType.kind)) {
                current.increaseElement();
                stackNew(new StackNode(current.dataType.valueType, byteBuf.writerIndex(), findStruct(current.dataType.valueType.qualifiedName, service)));
            }
        }

        @Override
        public void onEndArray() throws TException {
            assert isCollectionKind(current.dataType.kind);

            switch (current.dataType.kind) {
                case LIST:
                    oproto.writeListEnd();
                    reWriteByteBuf();
                    break;
                case SET:
                    oproto.writeSetEnd();
                    reWriteByteBuf();
                    break;
            }
            if (isCollectionKind(peek().dataType.kind)) {
                pop();
            }
        }

        @Override
        public void onStartField(String name) throws TException {
            Field field = findField(name, current.struct);
            if (field == null) return;

            stackNew(new StackNode(field.dataType, byteBuf.writerIndex(), findStruct(field.dataType.qualifiedName, service)));
            oproto.writeFieldBegin(new TField(field.name, dataType2Byte(field.dataType), (short) field.getTag()));
        }

        @Override
        public void onEndField() throws TException {
            //TODO field == null的情况
            pop();
            oproto.writeFieldEnd();
        }

        @Override
        public void onBoolean(boolean value) throws TException {
            if (isMultiElementKind(current.dataType.kind)) {
                current.increaseElement();
            }
            oproto.writeBool(value);
        }

        @Override
        public void onNumber(double value) throws TException {
            if (isMultiElementKind(current.dataType.kind)) {
                current.increaseElement();
            }
            switch (current.dataType.kind) {
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
                case BIGDECIMAL:
                    //TODO
                    break;
                case BYTE:
                    //TODO
                    break;
                default:
                    throw new TException("DataType(" + current.dataType.kind + ") for " + current.dataType.qualifiedName + " is not a Number");

            }
        }

        @Override
        public void onNull() throws TException {
            //重置writerIndex
            byteBuf.writerIndex(current.byteBufPosition);
        }

        @Override
        public void onString(String value) throws TException {
            if (isMultiElementKind(current.dataType.kind)) {
                current.increaseElement();
            }
            oproto.writeString(value);
        }

        private void stackNew(StackNode node) {
            history.push(this.current);
            this.current = node;
        }

        private StackNode pop() {
            return this.current = history.pop();
        }

        private StackNode peek() {
            return history.peek();
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

        /**
         * 根据current 节点重写集合元素长度
         */
        private void reWriteByteBuf() throws TException {
            assert isMultiElementKind(current.dataType.kind);

            //拿到当前node的开始位置以及集合元素大小
            int beginPosition = current.byteBufPosition;
            int elCount = current.elCount;

            //备份最新的writerIndex
            int currentIndex = byteBuf.writerIndex();

            //reWriteListBegin
            byteBuf.writerIndex(beginPosition);

            switch (current.dataType.kind) {
                case MAP:
                    oproto.writeMapBegin(new TMap(dataType2Byte(current.dataType.keyType), dataType2Byte(current.dataType.valueType), elCount));
                    break;
                case SET:
                    oproto.writeSetBegin(new TSet(dataType2Byte(current.dataType.valueType), elCount));
                    break;
                case LIST:
                    oproto.writeListBegin(new TList(dataType2Byte(current.dataType.valueType), elCount));
                    break;
            }

            byteBuf.writerIndex(currentIndex);
        }

        /**
         * 是否集合类型
         *
         * @param kind
         * @return
         */
        private boolean isCollectionKind(DataType.KIND kind) {
            return kind == DataType.KIND.LIST || kind == DataType.KIND.SET;
        }

        /**
         * 是否容器类型
         *
         * @param kind
         * @return
         */
        private boolean isMultiElementKind(DataType.KIND kind) {
            return isCollectionKind(kind) || kind == DataType.KIND.MAP;
        }

        /**
         * 是否复杂类型
         *
         * @param kind
         * @return
         */
        private boolean isComplexKind(DataType.KIND kind) {
            return isMultiElementKind(kind) || kind == DataType.KIND.STRUCT;
        }
    }

    // json -> thrift
    @Override
    public void write(String input, TProtocol oproto) throws TException {
        new JsonParser(input, new Json2ThriftCallback(oproto)).parseJsValue();
    }

    @Override
    public void validate(String s) throws TException {

    }

    @Override
    public String toString(String s) {
        return s;
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

    private Struct findStruct(String qualifiedName, Service service) {
        if (qualifiedName == null) {
            return null;
        }
        List<Struct> structDefinitions = service.getStructDefinitions();

        for (Struct structDefinition : structDefinitions) {
            if ((structDefinition.getNamespace() + "." + structDefinition.getName()).equals(qualifiedName)) {
                return structDefinition;
            }
        }

        return null;
    }

}
