package com.isuwang.dapeng.remoting.fake.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.isuwang.dapeng.core.metadata.*;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.*;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础序列化
 *
 * @author craneding
 * @date 15/4/26
 */
public abstract class TBaseBeanSerializer implements TBeanSerializer<InvocationInfo> {

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
            if (enumItem.getLabel().equals(label))
                return enumItem.getValue();
        }

        for (TEnum.EnumItem enumItem : enumItems) {
            if (String.valueOf(enumItem.getValue()).equals(label))
                return enumItem.getValue();
        }

        return null;
    }

    protected String findEnumItemLabel(TEnum tEnum, Integer value) {
        Integer enumValue = null;
        List<TEnum.EnumItem> enumItems = tEnum.getEnumItems();
        for (TEnum.EnumItem enumItem : enumItems) {
            if (enumItem.getValue() == value)
                return enumItem.getLabel();
        }

        return null;
    }

    protected TEnum findEnum(String qualifiedName, Service service) {
        List<TEnum> enumDefinitions = service.getEnumDefinitions();

        for (TEnum enumDefinition : enumDefinitions) {
            if ((enumDefinition.getNamespace() + "." + enumDefinition.getName()).equals(qualifiedName))
                return enumDefinition;
        }

        return null;
    }

    protected Struct findStruct(String qualifiedName, Service service) {
        List<Struct> structDefinitions = service.getStructDefinitions();

        for (Struct structDefinition : structDefinitions) {
            if ((structDefinition.getNamespace() + "." + structDefinition.getName()).equals(qualifiedName))
                return structDefinition;
        }

        return null;
    }

    protected Method findMethod(String methodName, Service service) {
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (method.getName().equals(methodName))
                return method;
        }

        return null;
    }

    protected Field findField(String fieldName, Struct struct) {
        List<Field> fields = struct.getFields();

        for (Field field : fields) {
            if (field.getName().equals(fieldName))
                return field;
        }

        return null;
    }

    protected Field findField(int tag, Struct struct) {
        List<Field> fields = struct.getFields();

        for (Field field : fields) {
            if (field.getTag() == tag)
                return field;
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

}
