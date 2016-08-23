package com.isuwang.dapeng.core;

import java.util.Optional;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */
public class InvocationContext extends Context {

    public static class Factory {
        private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();
        private static ISoaHeaderProxy soaHeaderProxy;

        public static interface ISoaHeaderProxy {

            Optional<String> callerFrom();

            Optional<Integer> customerId();

            Optional<String> customerName();

            Optional<Integer> operatorId();

            Optional<String> operatorName();

            Optional<String> sessionId();
        }

        public static void setSoaHeaderProxy(ISoaHeaderProxy soaHeaderProxy) {
            Factory.soaHeaderProxy = soaHeaderProxy;
        }

        public static ISoaHeaderProxy getSoaHeaderProxy() {
            return soaHeaderProxy;
        }

        public static InvocationContext getNewInstance() {
            return new InvocationContext();
        }

        public static InvocationContext setCurrentInstance(InvocationContext context) {
            threadLocal.set(context);

            return context;
        }

        public static InvocationContext getCurrentInstance() {
            InvocationContext context = threadLocal.get();

            if (context == null) {
                context = getNewInstance();

                threadLocal.set(context);
            }

            return context;
        }

        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }

}
