package com.isuwang.dapeng.core.log;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.core.SoaHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * Created by admin on 2017/9/1.
 */
public class LogUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);


    /**
     * args[0]=serviceName,args[1]=versionName
     */
    public static void logInfo(Class<?>targetClass,String format,String ...args){
        try {
            ProcessorKey key = new ProcessorKey(args[0],args[1]);
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);
            if (appClassLoader!=null) {
                Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
                Method getILoggerFactory = logFactoryClass.getMethod("getLogger",Class.class);
                getILoggerFactory.setAccessible(true);
                Object obj = getILoggerFactory.invoke(null,targetClass); // Logger
                Object[] parameters= new Object[args.length];
                for (int i=0;i<args.length;i++) {
                    parameters[i]=args[i];
                }
                Method info = obj.getClass().getMethod("info",String.class,Object[].class);
                info.invoke(obj,format,parameters);
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }

    }

    public static void logError(Class<?>targetClass, SoaHeader soaHeader,String errMsg){
        try {
            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName());
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);

            if (appClassLoader!=null) {
                Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
                Method getILoggerFactory = logFactoryClass.getMethod("getLogger",Class.class);
                getILoggerFactory.setAccessible(true);
                Object obj = getILoggerFactory.invoke(null,targetClass); // Logger

                Method info = obj.getClass().getMethod("error",String.class);
                info.invoke(obj,errMsg);
            }

        }catch (Exception e){
            e.printStackTrace();
            LOGGER.error(e.getMessage());
        }
    }

}