package com.isuwang.dapeng.doc.cache;


import com.google.common.collect.TreeMultimap;
import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.metadata.Field;
import com.isuwang.dapeng.core.metadata.Method;
import com.isuwang.dapeng.core.metadata.Struct;
import com.isuwang.dapeng.core.metadata.TEnum;
import com.isuwang.dapeng.registry.RegistryAgentProxy;
import com.isuwang.dapeng.remoting.fake.metadata.MetadataClient;
import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.TProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Service Cache
 *
 * @author craneding
 * @date 15/4/26
 */
public class ServiceCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCache.class);

    private static Map<String, com.isuwang.dapeng.core.metadata.Service> services = new TreeMap<>();

    private static Map<String, com.isuwang.dapeng.core.metadata.Service> fullNameService = new TreeMap<>();

    public static TreeMultimap<String, String> urlMappings = TreeMultimap.create();

    public void init() {
        new Thread() {
            @Override
            public void run() {
                // 延迟10秒
                try {
                    Thread.sleep(10 * 1000);
                } catch (InterruptedException e) {
                }

                reloadServices();
            }
        }.start();
    }

    public void reloadServices() {
        final Map<String, com.isuwang.dapeng.core.metadata.Service> services = new TreeMap<>();
        urlMappings.clear();

        Map<ProcessorKey, TProcessor<?>> processorMap = RegistryAgentProxy.getCurrentInstance(RegistryAgentProxy.Type.Server).getProcessorMap();

        Set<ProcessorKey> keys = processorMap.keySet();
        for (ProcessorKey key : keys) {
            TProcessor<?> processor = processorMap.get(key);
            if (processor.getInterfaceClass().getClass() != null) {

                Service service = processor.getInterfaceClass().getAnnotation(Service.class);

                String serviceName = service.name();
                String version = service.version();


                String metadata = "";
                try {
                    metadata = new MetadataClient(serviceName, version).getServiceMetadata();
                } catch (TException e) {
                    LOGGER.error(e.getMessage(), e);
                }

                if (metadata != null) {
                    try (StringReader reader = new StringReader(metadata)) {
                        com.isuwang.dapeng.core.metadata.Service serviceData = JAXB.unmarshal(reader, com.isuwang.dapeng.core.metadata.Service.class);
                        loadResource(serviceData, services);
                    } catch (Exception e) {
                        LOGGER.error("生成SERVICE出错", e);
                    }
                }
            }
        }
        this.services = services;

        LOGGER.info("size of urlMapping: " + urlMappings.size());
    }

    public void destory() {
        services.clear();
    }

    public void loadResource(com.isuwang.dapeng.core.metadata.Service service, Map<String, com.isuwang.dapeng.core.metadata.Service> services) {

        String key = getKey(service);
        services.put(key, service);

        String fullNameKey = getFullNameKey(service);
        fullNameService.put(fullNameKey, service);

        //将service和service中的方法、结构体、枚举和字段名分别设置对应的url，以方便搜索
        urlMappings.put(service.getName(), "api/service/" + service.name + "/" + service.meta.version + ".htm");
        List<Method> methods = service.getMethods();
        for (int i = 0; i < methods.size(); i++) {
            Method method = methods.get(i);
            urlMappings.put(method.name, "api/method/" + service.name + "/" + service.meta.version + "/" + method.name + ".htm");
        }

        List<Struct> structs = service.getStructDefinitions();
        for (int i = 0; i < structs.size(); i++) {
            Struct struct = structs.get(i);
            urlMappings.put(struct.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");

            List<Field> fields = struct.getFields();
            for (int j = 0; j < fields.size(); j++) {
                Field field = fields.get(j);
                urlMappings.put(field.name, "api/struct/" + service.name + "/" + service.meta.version + "/" + struct.namespace + "." + struct.name + ".htm");
            }
        }

        List<TEnum> tEnums = service.getEnumDefinitions();
        for (int i = 0; i < tEnums.size(); i++) {
            TEnum tEnum = tEnums.get(i);
            urlMappings.put(tEnum.name, "api/enum/" + service.name + "/" + service.meta.version + "/" + tEnum.namespace + "." + tEnum.name + ".htm");
        }

    }

    public com.isuwang.dapeng.core.metadata.Service getService(String name, String version) {

        if (name.contains("."))
            return fullNameService.get(getKey(name, version));
        else
            return services.get(getKey(name, version));
    }

    private String getKey(com.isuwang.dapeng.core.metadata.Service service) {
        return getKey(service.getName(), service.getMeta().version);
    }

    private String getFullNameKey(com.isuwang.dapeng.core.metadata.Service service) {
        return getKey(service.getNamespace() + "." + service.getName(), service.getMeta().version);
    }

    private String getKey(String name, String version) {
        return name + ":" + version;
    }

    public Map<String, com.isuwang.dapeng.core.metadata.Service> getServices() {
        return services;
    }

}
