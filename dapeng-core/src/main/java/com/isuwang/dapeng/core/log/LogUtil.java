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




//    public static void logInfo(Class<?>logClass,SoaHeader soaHeader,String format,Object ...args){
//        try {
//            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName());
//            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);
//            if (appClassLoader!=null) {
//                Object logger=null;
//                String methonName="info";
//                Method infoMethod=getMethod(appClassLoader,methonName,logClass,logger);
//                infoMethod.invoke(logger,format,args);
//            }else{
//                Logger logger = LoggerFactory.getLogger(logClass);
//                logger.info(format,args);
//            }
//        } catch (Exception e) {
//            LOGGER.error(e.getMessage());
//            Logger logger = LoggerFactory.getLogger(logClass);
//            logger.info(format,args);
//        }
//
//    }
    public static void logInfo(Class<?>targetClass,SoaHeader soaHeader,String format,String ...args){
        try {
            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(),soaHeader.getVersionName());
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


    public static void logError(Class<?>logClass, SoaHeader soaHeader,String errMsg,Throwable exception){
        try {
            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName());
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);

            if (appClassLoader!=null) {
                Object logger=null;
                String methonName="error";
                Method infoMethod=getMethod(appClassLoader,methonName,logClass,logger);
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



    public static Method getMethod(ClassLoader appClassLoader,String methonName,Class<?>logClass,Object logger)throws Exception{
        Method method;
        if (loggerMap.containsKey(logClass.getName())){
            logger=loggerMap.get(logClass.getName());
        }else{
            Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
            Method getILoggerFactory = logFactoryClass.getMethod("getLogger", Class.class);
            getILoggerFactory.setAccessible(true);
            logger=getILoggerFactory.invoke(null, logClass);
            loggerMap.put(logClass.getName(),logger);
        }
        String logMethodKey= logClass.getName()+methonName;
        if (logMethodMap.containsKey(logMethodKey)){
            method=logMethodMap.get(logMethodKey);
        }else{
            method=logger.getClass().getMethod(methonName,String.class,Throwable.class);
            logMethodMap.put(logMethodKey,method);
        }
        return method;
    }

}