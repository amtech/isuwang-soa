package com.isuwang.dapeng.container.registry;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.org.apache.thrift.TProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author craneding
 * @date 16/3/13
 */
public class ProcessorCache {

    private static final Map<ProcessorKey, TProcessor<?>> processorMap = new ConcurrentHashMap<>();

    public static Map<ProcessorKey, TProcessor<?>> getProcessorMap() {
        return processorMap;
    }

}
