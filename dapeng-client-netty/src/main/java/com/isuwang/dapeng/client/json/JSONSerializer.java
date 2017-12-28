package com.isuwang.dapeng.client.json;

import com.google.gson.*;
import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.core.metadata.*;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JSON序列化
 *
 * @author craneding
 * @date 15/4/26
 */
public class JSONSerializer implements BeanSerializer {

    @Override
    public void validate(Object bean) throws TException {

    }

    @Override
    public String toString(Object bean) {
        return bean.toString();
    }

    private Service service;

    public void setService(Service service) {
        this.service = service;
    }

    private DataInfo dataInfo;

    public void setDataInfo(DataInfo dataInfo) {
        this.dataInfo = dataInfo;
    }

    @Override
    public String read(TProtocol iprot) throws TException {

        JsonObject responseJSON = new JsonObject();
        JsonArray jsonArray = new JsonArray();

        iprot.readStructBegin();

        TField schemeField;

        while (true) {

            schemeField = iprot.readFieldBegin();
            if (schemeField.type == TType.STOP) {
                break;
            }

            if (schemeField.id == 0) {
                List<Field> fields = dataInfo.getMethod().getResponse().getFields();

                Field field = fields.isEmpty() ? null : fields.get(0);
                if (field != null) {
                    DataType dataType = field.getDataType();
                    readField(iprot, null, dataType, jsonArray, schemeField, service);
                } else {
                    TProtocolUtil.skip(iprot, schemeField.type);
                }

            } else if (schemeField.id == 1) {

                String errCode = "", errMsg = "";
                iprot.readStructBegin();
                while (true) {
                    schemeField = iprot.readFieldBegin();

                    if (schemeField.type == TType.STOP) {
                        break;
                    }
                    switch (schemeField.id) {
                        case 1: // ERR_CODE
                            if (schemeField.type == TType.STRING) {
                                errCode = iprot.readString();
                            } else {
                                TProtocolUtil.skip(iprot, schemeField.type);
                            }
                            break;
                        case 2: // ERR_MSG
                            if (schemeField.type == TType.STRING) {
                                errMsg = iprot.readString();
                            } else {
                                TProtocolUtil.skip(iprot, schemeField.type);
                            }
                            break;
                        default:
                            TProtocolUtil.skip(iprot, schemeField.type);
                    }
                    iprot.readFieldEnd();
                }
                iprot.readStructEnd();

                throw new SoaException(errCode, errMsg);

            } else {
                TProtocolUtil.skip(iprot, schemeField.type);
            }
            iprot.readFieldEnd();
        }
        iprot.readStructEnd();

        responseJSON.add("success", jsonArray.size() > 0 ? jsonArray.get(0) : null);
        responseJSON.addProperty("responseCode", "0000");
        responseJSON.addProperty("responseMsg", "成功");

        return responseJSON.toString();
    }

    @Override
    public void write(Object request, TProtocol oprot) throws TException {

        JsonObject jsonObject = new JsonParser().parse((String)request).getAsJsonObject();

        JsonElement serviceName = jsonObject.get("serviceName");
        JsonElement version = jsonObject.get("version");
        JsonElement methodName = jsonObject.get("methodName");
        JsonElement params = jsonObject.get("params");
        JsonObject methodParamers = new JsonObject();

        if (serviceName == null) {
            throw new TException("not fund service name in request.");
        }

        if (version == null) {
            throw new TException("not fund service version in request.");
        }

        if (methodName == null) {
            throw new TException("not fund method name in request.");
        }

        if (service == null) {
            throw new TException("not fund service(" + serviceName.getAsString() + "," + version.getAsString() + ") in cache.");
        }

        oprot.writeStructBegin(new TStruct(methodName.getAsString()));
        if (params == null) {
            throw new TException("not fund params in request.");
        }

        Set<Map.Entry<String, JsonElement>> entries = new LinkedHashSet<>(methodParamers.entrySet());
        entries.addAll(params.getAsJsonObject().entrySet());

        Method method = null;
        for (Method tempMethod: service.getMethods()) {
            if (tempMethod.getName().equals(methodName.getAsString())) {
                method = tempMethod;
                break;
            }
        }
        if (method == null) {
            throw new TException("not fund method: " + methodName.getAsString() + " in request's method.");
        }

        for (Map.Entry<String, JsonElement> entry : entries) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();

            Field field = findField(key, method.getRequest());

            if (field == null) {
                throw new TException("not fund " + key + " in request's method.");
            }

            oprot.writeFieldBegin(new TField(field.getName(), dataType2Byte(field.getDataType()), (short) field.getTag()));
            writeField(service, field.getDataType(), oprot, value);
            oprot.writeFieldEnd();
        }

        oprot.writeFieldStop();
        oprot.writeStructEnd();
    }

    protected void writeField(Service service, DataType dataType, TProtocol oprot, Object value) throws TException {
        final boolean isJsonElement = value instanceof JsonElement;
        final JsonElement jsonElement = isJsonElement ? (JsonElement) value : null;

        switch (dataType.getKind()) {
            case VOID:
                break;
            case BOOLEAN:
                oprot.writeBool(isJsonElement ? jsonElement.getAsBoolean() : Boolean.valueOf(value.toString()));
                break;
            case BYTE:
                oprot.writeByte(isJsonElement ? jsonElement.getAsByte() : Byte.valueOf(value.toString()));
                break;
            case SHORT:
                oprot.writeI16(isJsonElement ? jsonElement.getAsShort() : Short.valueOf(value.toString()));
                break;
            case INTEGER:
                oprot.writeI32(isJsonElement ? jsonElement.getAsInt() : Integer.valueOf(value.toString()));
                break;
            case LONG:
                oprot.writeI64(isJsonElement ? jsonElement.getAsLong() : Long.valueOf(value.toString()));
                break;
            case DOUBLE:
                oprot.writeDouble(isJsonElement ? jsonElement.getAsDouble() : Double.valueOf(value.toString()));
                break;
            case STRING:
                oprot.writeString(value instanceof JsonObject ? value.toString() : (isJsonElement ? jsonElement.getAsString() : value.toString()));
                break;
            case BINARY:
                String tmp = value instanceof JsonObject ? value.toString() : (isJsonElement ? jsonElement.getAsString() : value.toString());
                oprot.writeBinary(ByteBuffer.wrap(tmp.getBytes()));
                break;
            case DATE:
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
                Long time = 0L;
                try {
                    if (isJsonElement)
                        time = sdf.parse(jsonElement.getAsString()).getTime();
                    else
                        time = sdf.parse(value.toString()).getTime();
                } catch (ParseException e) {
                }
                oprot.writeI64(time);
                break;
            case BIGDECIMAL:
                String bigDecimal = isJsonElement ? jsonElement.getAsBigDecimal().toString() : new BigDecimal((String) value).toString();
                oprot.writeString(bigDecimal);
                break;
            case MAP: {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                int size = jsonObject.entrySet().size();

                if (size >= 0) {
                    oprot.writeMapBegin(new TMap(dataType2Byte(dataType.keyType), dataType2Byte(dataType.valueType), size));

                    for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                        String key = entry.getKey();
                        Object obj = entry.getValue();

                        writeField(service, dataType.keyType, oprot, key);
                        writeField(service, dataType.valueType, oprot, obj);
                    }

                    oprot.writeMapEnd();
                }
            }
            break;
            case LIST: {
                if (jsonElement.isJsonArray()) {
                    JsonArray jsonArray = jsonElement.getAsJsonArray();

                    int size = jsonArray.size();

                    if (size >= 0) {
                        oprot.writeListBegin(new TList(dataType2Byte(dataType.getValueType()), size));

                        for (int i = 0; i < size; i++) {
                            writeField(service, dataType.getValueType(), oprot, jsonArray.get(i));
                        }

                        oprot.writeListEnd();
                    }
                } else if (jsonElement instanceof JsonObject) {
                    Set<Map.Entry<String, JsonElement>> entries = ((JsonObject) jsonElement).entrySet();
                    if (!entries.isEmpty()) {
                        oprot.writeListBegin(new TList(dataType2Byte(dataType.getValueType()), entries.size()));

                        for (Map.Entry<String, JsonElement> entry : entries) {
                            writeField(service, dataType.getValueType(), oprot, entry.getValue());
                        }

                        oprot.writeListEnd();
                    }
                } else {
                    throw new TException(value + " is must be List");
                }
            }
            break;
            case SET: {
                if (jsonElement.isJsonArray()) {
                    JsonArray jsonArray = jsonElement.getAsJsonArray();

                    int size = jsonArray.size();

                    if (size >= 0) {
                        oprot.writeSetBegin(new TSet(dataType2Byte(dataType.getValueType()), size));

                        for (int i = 0; i < size; i++) {
                            writeField(service, dataType.getValueType(), oprot, jsonArray.get(i));
                        }

                        oprot.writeListEnd();
                    }
                } else if (jsonElement instanceof JsonObject) {
                    Set<Map.Entry<String, JsonElement>> entries = ((JsonObject) jsonElement).entrySet();
                    if (!entries.isEmpty()) {
                        oprot.writeSetBegin(new TSet(dataType2Byte(dataType.getValueType()), entries.size()));

                        for (Map.Entry<String, JsonElement> entry : entries) {
                            writeField(service, dataType.getValueType(), oprot, entry.getValue());
                        }

                        oprot.writeListEnd();
                    }
                } else {
                    throw new TException(value + " is must be Set");
                }
            }
            break;
            case ENUM:
                TEnum tEnum = findEnum(dataType.getQualifiedName(), service);

                oprot.writeI32(findEnumItemValue(tEnum, jsonElement.getAsString()));

                break;
            case STRUCT:
                Struct struct = findStruct(dataType.getQualifiedName(), service);

                if (struct == null)
                    throw new TException("not fund " + dataType.getQualifiedName() + " in request(" + service.getName() + ")");

                oprot.writeStructBegin(new TStruct(struct.getName()));

                Set<Map.Entry<String, JsonElement>> entries = jsonElement.getAsJsonObject().entrySet();
                if (entries != null) {
                    for (Map.Entry<String, JsonElement> entry : entries) {
                        Field field1 = findField(entry.getKey(), struct);

                        if (field1 == null)
                            throw new TException("not fund " + entry.getKey() + " in request(" + struct.getName() + ")");

                        oprot.writeFieldBegin(new TField(field1.getName(), dataType2Byte(field1.getDataType()), (short) field1.getTag()));

                        if (field1.isOptional()) {

                            if (!entry.getValue().toString().equals("{}")) {
                                if (entry.getValue() instanceof JsonPrimitive)
                                    writeField(service, field1.getDataType(), oprot, entry.getValue());
                                else if (entry.getValue() instanceof JsonObject) {
                                    if (field1.getDataType().getKind() == DataType.KIND.STRUCT)
                                        writeField(service, field1.getDataType(), oprot, entry.getValue().getAsJsonObject());
                                    else
                                        writeField(service, field1.getDataType(), oprot, entry.getValue().getAsJsonObject().get("value"));
                                } else if (entry.getValue() instanceof JsonElement) {
                                    writeField(service, field1.getDataType(), oprot, entry.getValue());
                                }
                            }
                        } else
                            writeField(service, field1.getDataType(), oprot, entry.getValue());

                        oprot.writeFieldEnd();
                    }
                }

                oprot.writeFieldStop();
                oprot.writeStructEnd();
                break;
        }
    }

    protected Integer findEnumItemValue(TEnum tEnum, String label) {
        List<TEnum.EnumItem> enumItems = tEnum.getEnumItems();
        for (TEnum.EnumItem enumItem : enumItems) {
            if (enumItem.getLabel().equals(label)) {
                return enumItem.getValue();
            }
        }

        for (TEnum.EnumItem enumItem : enumItems) {
            if (String.valueOf(enumItem.getValue()).equals(label)) {
                return enumItem.getValue();
            }
        }

        return null;
    }

    protected String findEnumItemLabel(TEnum tEnum, Integer value) {
        Integer enumValue = null;
        List<TEnum.EnumItem> enumItems = tEnum.getEnumItems();
        for (TEnum.EnumItem enumItem : enumItems) {
            if (enumItem.getValue() == value) {
                return enumItem.getLabel();
            }
        }

        return null;
    }

    protected TEnum findEnum(String qualifiedName, Service service) {
        List<TEnum> enumDefinitions = service.getEnumDefinitions();

        for (TEnum enumDefinition : enumDefinitions) {
            if ((enumDefinition.getNamespace() + "." + enumDefinition.getName()).equals(qualifiedName)) {
                return enumDefinition;
            }
        }

        return null;
    }

    protected Struct findStruct(String qualifiedName, Service service) {
        List<Struct> structDefinitions = service.getStructDefinitions();

        for (Struct structDefinition : structDefinitions) {
            if ((structDefinition.getNamespace() + "." + structDefinition.getName()).equals(qualifiedName)) {
                return structDefinition;
            }
        }

        return null;
    }

    protected Method findMethod(String methodName, Service service) {
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }

        return null;
    }

    protected Field findField(String fieldName, Struct struct) {
        List<Field> fields = struct.getFields();

        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }

        return null;
    }

    protected Field findField(int tag, Struct struct) {
        List<Field> fields = struct.getFields();

        for (Field field : fields) {
            if (field.getTag() == tag) {
                return field;
            }
        }

        return null;
    }

    public byte dataType2Byte(DataType type) {
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


    private JsonElement readField(TProtocol iprot, Field field, DataType dataType, JsonElement jsonElement, TField schemeField, Service service) throws TException {
        JsonElement value = null;

        switch (dataType.getKind()) {
            case VOID:
                break;
            case BOOLEAN:
                value = new JsonPrimitive(iprot.readBool());
                break;
            case BYTE:
                value = new JsonPrimitive(iprot.readByte());
                break;
            case SHORT:
                value = new JsonPrimitive(iprot.readI16());
                break;
            case INTEGER:
                value = new JsonPrimitive(iprot.readI32());
                break;
            case LONG:
                value = new JsonPrimitive(iprot.readI64());
                break;
            case DOUBLE:
                value = new JsonPrimitive(iprot.readDouble());
                break;
            case STRING:
                value = new JsonPrimitive(iprot.readString());
                break;
            case BINARY:
                ByteBuffer bf = iprot.readBinary();
                byte[] bytes = new byte[bf.remaining()];
                bf.get(bytes);

                value = new JsonPrimitive(new String(bytes));

//                CharBuffer charBuffer = null;
//                try {
//                    Charset charset = Charset.forName("UTF-8");
//                    CharsetDecoder decoder = charset.newDecoder();
//                    charBuffer = decoder.decode(bf.asReadOnlyBuffer());
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                    value = new JsonPrimitive("ByteBuffer转String出错，可能不是UTF-8编码");
//                }
//                if (charBuffer != null)
//                    value = new JsonPrimitive(charBuffer.toString());
                break;
            case DATE:
                Long time = iprot.readI64();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                java.util.Date date = new java.util.Date(time);
                value = new JsonPrimitive(sdf.format(date));
                break;
            case BIGDECIMAL:
                value = new JsonPrimitive(new BigDecimal(iprot.readString()));
                break;
            case MAP:
                //if(schemeField.type == TType.MAP) {
                TMap tMap = iprot.readMapBegin();

                JsonObject jsonMap = new JsonObject();
                for (int i = 0; i < tMap.size; i++) {
                    JsonElement keyElement = readField(iprot, null, dataType.getKeyType(), null, schemeField, service);
                    JsonElement valueElement = readField(iprot, null, dataType.getValueType(), null, schemeField, service);

                    jsonMap.add(keyElement.getAsString(), valueElement);
                }

                iprot.readMapEnd();

                value = jsonMap;
                break;
            case LIST:
//                if(schemeField.type == TType.LIST) {
                TList tList = iprot.readListBegin();

                JsonArray jsonElements = new JsonArray();
                for (int i = 0; i < tList.size; i++) {
                    readField(iprot, null, dataType.getValueType(), jsonElements, null, service);
                }

                iprot.readListEnd();

                value = jsonElements;
                break;
            case SET:
//                if(schemeField.type == TType.SET) {
                TSet tSet = iprot.readSetBegin();

                JsonArray jsonElements1 = new JsonArray();
                for (int i = 0; i < tSet.size; i++) {
                    readField(iprot, null, dataType.getValueType(), jsonElements1, schemeField, service);
                }

                iprot.readSetEnd();

                value = jsonElements1;
//
                break;
            case ENUM:
                TEnum tEnum = findEnum(dataType.getQualifiedName(), service);

                String enumItemLabel = findEnumItemLabel(tEnum, iprot.readI32());

                value = new JsonPrimitive(enumItemLabel);
                break;
            case STRUCT:
                iprot.readStructBegin();

                Struct struct = findStruct(dataType.getQualifiedName(), service);

                JsonObject jsonObject = new JsonObject();

                do {
                    TField tField = iprot.readFieldBegin();

                    if (tField.type == TType.STOP)
                        break;

                    Field field1 = findField(tField.id, struct);

                    readField(iprot, field1, field1.getDataType(), jsonObject, tField, service);
                } while (true);

                iprot.readFieldEnd();
                iprot.readStructEnd();

                value = jsonObject;
                break;
        }

        if (jsonElement != null) {
            if (jsonElement.isJsonArray()) {
                ((JsonArray) jsonElement).add(value);
            } else if (jsonElement.isJsonObject() && field != null) {
                ((JsonObject) jsonElement).add(field.getName(), value);
            }
        }
        return value;
    }

}
