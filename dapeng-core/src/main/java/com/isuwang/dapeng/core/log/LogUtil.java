package com.isuwang.dapeng.core.log;

import com.isuwang.dapeng.core.ProcessorKey;
import com.isuwang.dapeng.core.SoaBaseProcessor;
import com.isuwang.dapeng.core.SoaHeader;
import com.isuwang.dapeng.core.helper.SoaHeaderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Created by admin on 2017/9/1.
 */
public class LogUtil {

    private final static Logger LOGGER = LoggerFactory.getLogger(LogUtil.class);

    private static final Map<String, Object> loggerMap = new ConcurrentHashMap<>();

    private static final Map<String, Method> logMethodMap = new ConcurrentHashMap<>();

    public static void logInfo(Class<?> logClass, SoaHeader soaHeader, String formattedMsg, Object... args) {
        methodInvoke(logClass, soaHeader, "info", logger -> logger.info(formattedMsg, args), new Object[]{formattedMsg, args});
    }

    public static void logError(Class<?> logClass, SoaHeader soaHeader, String errMsg, Throwable exception) {
        methodInvoke(logClass, soaHeader, "error", logger -> logger.error(errMsg, exception), new Object[]{errMsg, exception});
    }

    public static void logDebug(Class<?> logClass, SoaHeader soaHeader, String formattedMsg, Object... args){
        methodInvoke(logClass,soaHeader,"debug",logger -> logger.debug(formattedMsg,args),new Object[]{formattedMsg, args});
    }

    private static void methodInvoke(Class<?> logClass, SoaHeader soaHeader, String methodName, Consumer<Logger> loggerConsumer, Object... args) {
        try {
            ProcessorKey key = new ProcessorKey(soaHeader.getServiceName(), soaHeader.getVersionName());
            ClassLoader appClassLoader = SoaAppClassLoaderCache.getAppClassLoaderMap().get(key);

            if (appClassLoader != null) {
                Object appLogger = getLogger(appClassLoader, logClass, appClassLoader.hashCode());
                Method infoMethod = getMethod(methodName, logClass, appLogger, appClassLoader.hashCode());
                infoMethod.invoke(appLogger, args);
            } else {
                Logger containerLogger = LoggerFactory.getLogger(logClass);
                loggerConsumer.accept(containerLogger);
            }

        } catch (Exception e) {
            //有异常用容器的logger打日志
            LOGGER.error(e.getMessage());
            Logger containerLogger = LoggerFactory.getLogger(logClass);
            loggerConsumer.accept(containerLogger);
        }
    }


    /**
     * 先从logMethodMap查，没有再利用反射获取Method
     */
    public static Method getMethod(String methodName, Class<?> logClass, Object logger, int classLoaderHex) throws Exception {
        Method method;
        String logMethodKey = classLoaderHex + "." + logClass.getName() + methodName;
        if (logMethodMap.containsKey(logMethodKey)) {
            method = logMethodMap.get(logMethodKey);
        } else {
            if (methodName.equals("error")) {
                method = logger.getClass().getMethod(methodName, String.class, Throwable.class);
            } else {
                method = logger.getClass().getMethod(methodName, String.class, Object[].class);
            }

            logMethodMap.put(logMethodKey, method);
        }
        return method;
    }

    /**
     * 先从loggerMap查，没有再利用反射获取logger
     */
    public static Object getLogger(ClassLoader appClassLoader, Class<?> logClass, int classLoaderHex) throws Exception {
        Object logger;
        String logMethodKey= classLoaderHex+"."+logClass.getName();
        if (loggerMap.containsKey(logMethodKey)) {
            logger = loggerMap.get(logMethodKey);
        } else {
            Class<?> logFactoryClass = appClassLoader.loadClass("org.slf4j.LoggerFactory");
            Method getILoggerFactory = logFactoryClass.getMethod("getLogger", Class.class);
            getILoggerFactory.setAccessible(true);
            logger = getILoggerFactory.invoke(null, logClass);
            loggerMap.put(logMethodKey, logger);
        }
        return logger;
    }

}