package com.isuwang.dapeng.core.log;

import com.isuwang.dapeng.core.ProcessorKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 2017/9/8.
 */
public class SoaAppClassLoaderCache {

    private static final Map<ProcessorKey,ClassLoader> appClassLoaderMap =new ConcurrentHashMap<>();

    public static Map<ProcessorKey,ClassLoader> getAppClassLoaderMap(){return appClassLoaderMap;}
}