package com.isuwang.dapeng.tools.helpers;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.isuwang.soa.core.metadata.*;

import java.nio.ByteBuffer;
import java.util.*;

/**
 * @author Eric on 2016/2/16.
 */
public class JsonRequestExampleHelper {

    public static void getRequestJson(String... args) {
        if (args.length != 4) {
            System.out.println("example: java -jar dapeng.jar json com.isuwang.soa.hello.service.HelloService 1.0.0 sayHello");
            System.exit(0);
        }
        String serviceName = args[1];
        String versionName = args[2];
        String methodName = args[3];

        System.out.println("Getting Service metadata ...");
        Service service = getService(serviceName, versionName, methodName);
        List<Struct> structs = getMethod(service, methodName);
        for (Struct struct : structs) {
            System.out.println("---------------------------------------------------------------");
            List<Field> parameters = struct.getFields();
            Map<String, Object> jsonSample = getJsonSample(service, parameters);
            System.out.println(gson_format.toJson(jsonSample));
        }
    }

    private static Service getService(String serviceName, String versionName, String methodName) {

        Service service = ServiceCache.getService(serviceName, versionName);
        return service;
    }

    private static List<Struct> getMethod(Service service, String methodName) {
        List<Struct> structs = new ArrayList<Struct>();
        List<Method> methods = service.getMethods();
        for (Method method : methods) {
            if (methodName.equals(method.getName())) {
                structs.add(method.getRequest());
            }
        }
        return structs;
    }


    private final static Gson gson_format = new GsonBuilder().setExclusionStrategies(new ExclusionStrategy() {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            return f.getName().equals("attachment") || f.getName().equals("__isset_bitfield");
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return clazz == ByteBuffer.class;
        }
    }).setPrettyPrinting().create();

    private static Map<String, Object> getJsonSample(Service service, List<Field> parameters) {
        String fieldName;
        DataType fieldType;
        Map<String, Object> mapTemp = new HashMap<String, Object>();
        for (int i = 0; i < parameters.size(); i++) {
            Field parameter = parameters.get(i);
            fieldName = parameter.getName();
            fieldType = parameter.getDataType();
            mapTemp.put(fieldName, fillValue(service, fieldType));
        }
        return mapTemp;
    }

    private static Object fillValue(Service service, DataType fieldType) {
        Object randomValue = null;
        switch (fieldType.getKind()) {
            case VOID:
                break;
            case BOOLEAN:
                randomValue = Math.round(Math.random()) == 1 ? "true" : "false";
                break;
            case BYTE:
                randomValue = (byte) (Math.random() * 256 - 128);
                break;
            case SHORT:
                randomValue = Math.round(Math.random() * 100);
                break;
            case INTEGER:
                randomValue = Math.round(Math.random() * 1000);
                break;
            case LONG:
                randomValue = Math.round(Math.random() * 1000);
                break;
            case DOUBLE:
                randomValue = Math.random() * 100;
                break;
            case STRING:
                randomValue = "sampleDataString";
                break;
            case BINARY:
                randomValue = "546869732049732041205465737420427974652041727261792E";
                break;
            case MAP:
                DataType keyType = fieldType.getKeyType();
                DataType valueType = fieldType.getValueType();
                Map<Object, Object> mapTemp = new HashMap<Object, Object>();
                Object key = fillValue(service, keyType);
                Object value = fillValue(service, valueType);
                mapTemp.put(key, value);

                randomValue = mapTemp;
                break;
            case LIST:
                List list = new ArrayList<Object>();
                DataType listValueType = fieldType.getValueType();
                list.add(fillValue(service, listValueType));
                list.add(fillValue(service, listValueType));

                randomValue = list;
                break;
            case SET:
                Set set = new HashSet<Object>();
                DataType setValueType = fieldType.getValueType();
                set.add(fillValue(service, setValueType));
                set.add(fillValue(service, setValueType));
                randomValue = set;
                break;
            case ENUM:
                List<TEnum> structsE = service.getEnumDefinitions();
                for (int i = 0; i < structsE.size(); i++) {
                    TEnum tenum = structsE.get(i);
                    if ((tenum.getNamespace() + "." + tenum.getName()) == fieldType.qualifiedName) {
                        int size = tenum.enumItems.size();
                        int index = (int) (Math.random() * size);
                        return tenum.enumItems.get(index).label;
                    }
                }
                return "";
            case STRUCT:
                List<Struct> structs = service.getStructDefinitions();
                for (int i = 0; i < structs.size(); i++) {
                    Struct struct = structs.get(i);
                    if ((struct.getNamespace() + '.' + struct.getName()).equals(fieldType.getQualifiedName())) {
                        randomValue = getJsonSample(service, struct.getFields());
                    }
                }
                break;

            case DATE:
                randomValue = "2016/06/16 16:00";
                break;
            case BIGDECIMAL:
                randomValue = "1234567.123456789123456";
                break;
            default:
                randomValue = "";
        }
        return randomValue;
    }
}
