package com.isuwang.dapeng.core.log;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 2017/9/1.
 */
public class LogUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

    private static final Map<String, Object> loggerMap = new ConcurrentHashMap<>();

    private static final Map<String,Method> logMethodMap = new ConcurrentHashMap<>();




    public static void logInfo(Class<?>logClass,SoaHeader soaHeader,String format,Object ...args){
        try {

            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName());
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);
            int classLoaderHex=appClassLoader.hashCode();
            if (appClassLoader!=null) {
                Object logger=getLogger(appClassLoader,logClass,classLoaderHex);
                String methonName="info";
                Method infoMethod=getMethod(methonName,logClass,logger,classLoaderHex);
                infoMethod.invoke(logger,format,args);
            }else{
                Logger logger = LoggerFactory.getLogger(logClass);
                logger.info(format,args);
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            Logger logger = LoggerFactory.getLogger(logClass);
            logger.info(format,args);
        }

    }



    public static void logError(Class<?>logClass, SoaHeader soaHeader,String errMsg,Throwable exception){
        try {
            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName());
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);

            if (appClassLoader!=null) {
                Object logger=null;
                String methonName="error";
                Method infoMethod=getMethod(methonName,logClass,logger,appClassLoader.hashCode());
                infoMethod.invoke(logger,errMsg,exception);
            }else{
                Logger logger = LoggerFactory.getLogger(logClass);
                logger.error(errMsg,exception);
            }

        }catch (Exception e){
            LOGGER.error(e.getMessage());
            Logger logger = LoggerFactory.getLogger(logClass);
            logger.error(errMsg,exception);
        }
    }



    public static Method getMethod(String methonName,Class<?>logClass,Object logger,int classLoaderHex)throws Exception{
        Method method;
        String logMethodKey= classLoaderHex+"."+logClass.getName()+methonName;
        if (logMethodMap.containsKey(logMethodKey)){
            method=logMethodMap.get(logMethodKey);
        }else{
            if (methonName.equals("error")){
                method=logger.getClass().getMethod(methonName,String.class,Throwable.class);
            }else{
                method=logger.getClass().getMethod(methonName,String.class,Object[].class);
            }

            logMethodMap.put(logMethodKey,method);
        }
        return method;
    }

    public static Object getLogger(ClassLoader appClassLoader,Class<?>logClass,int classLoaderHex) throws Exception{
        Object logger;
        if (loggerMap.containsKey(logClass.getName())){
            logger=loggerMap.get(logClass.getName());
        }else{
            Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
            Method getILoggerFactory = logFactoryClass.getMethod("getLogger", Class.class);
            getILoggerFactory.setAccessible(true);
            logger=getILoggerFactory.invoke(null, logClass);
            loggerMap.put(classLoaderHex+"."+logClass.getName(),logger);
        }
        return logger;
    }

}