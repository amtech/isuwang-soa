package com.isuwang.dapeng.json;

import com.isuwang.dapeng.core.metadata.DataType;
import com.isuwang.dapeng.core.metadata.Field;
import com.isuwang.dapeng.core.metadata.Struct;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TField;
import com.isuwang.org.apache.thrift.protocol.TProtocol;
import com.isuwang.org.apache.thrift.protocol.TType;

import java.util.Stack;

public class JsonSerializer {

    private final Struct struct;

    public JsonSerializer(Struct struct){
        this.struct = struct;
    }

    // thrift -> json
    public void read(TProtocol iproto, JsonCallback writer) throws TException {
        iproto.readStructBegin();

        while(true){
            TField field = iproto.readFieldBegin();
            if(field.type == TType.STOP)
                break;

            Field fld = struct.getFields().get(0); // TODO get fld by field.id
            boolean skip = fld == null;

            switch(field.type) {
                case TType.VOID:
                    break;
                case TType.BOOL:
                    boolean value = iproto.readBool();
                    if(!skip){
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
                    if(!skip) {
                        String subStructName = fld.dataType.qualifiedName;
                        Struct subStruct = null;    // findStruct(subStructName)
                        new JsonSerializer(subStruct).read(iproto, writer);
                    }
                    else {
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

    public JsonCallback read(TProtocol iproto) throws TException {

        JsonWriter writer = new JsonWriter();
        read(iproto, writer);
        return writer;
    }

    static class Json2ThriftCallback implements JsonCallback {

        private final TProtocol oproto;


        // current struct
        DataType current;

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
        Stack<DataType> history = new Stack<>();


        public Json2ThriftCallback(TProtocol oproto){
            this.oproto = oproto;
        }


        private void stackNew(DataType type) {
            history.push(this.current);
            this.current = current;
        }
        private DataType pop() {
            return history.pop();
        }

        @Override
        public void onStartObject() {

        }

        @Override
        public void onEndObject() {

        }

        @Override
        public void onStartArray() {
            stackNew(current.valueType);
        }

        @Override
        public void onEndArray() {
            pop();
        }

        @Override
        public void onStartField(String name) {
            // if field is Struct, stackNew(subStruct)
            // if field is List
            Field field = null;
            stackNew(field.dataType);
        }

        @Override
        public void onEndField() {
            pop();
        }

        @Override
        public void onBoolean(boolean value) {

        }

        @Override
        public void onNumber(double value) {

        }

        @Override
        public void onNull() {

        }

        @Override
        public void onString(String value) {

        }
    }

    // json -> thrift
    public void write(CharSequence input, TProtocol oproto) throws TException {


        new JsonReader(input, new Json2ThriftCallback(oproto));

    }


}
