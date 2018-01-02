package com.isuwang.dapeng.core;

import com.isuwang.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */

public class InvocationContextImpl implements  InvocationContext {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private Optional<String> calleeIp;

    private Optional<Integer> calleePort;

    private InvocationInfo invocationInfo;

    /**
     * 全局事务id
     */
    private Optional<Integer> transactionId = Optional.empty();

    // readonly
    private int seqid;

    // read/write
    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    @Override
    public Optional<String> getCalleeIp() {
        return this.calleeIp;
    }

    @Override
    public void setCalleeIp(Optional<String> calleeIp) {
        this.calleeIp = calleeIp;
    }

    @Override
    public Optional<Integer> getCalleePort() {
        return this.calleePort;
    }

    @Override
    public void setCalleePort(Optional<Integer> calleePort) {
        this.calleePort = calleePort;
    }

    @Override
    public InvocationInfo getLastInfo() {
        return this.invocationInfo;
    }

    @Override
    public void setLastInfo(InvocationInfo invocationInfo) {
        this.invocationInfo = invocationInfo;
    }

    public Optional<Integer> getTransactionId() {
        return transactionId;
    }

    @Override
    public void setTransactionId(Optional<Integer> transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public int getSeqid() {
        return seqid;
    }

    public void setSeqid(int seqid) {
        this.seqid = seqid;
    }

    public static class Factory {
        private static ThreadLocal<InvocationContext> threadLocal = new ThreadLocal<>();

        public static InvocationContext createNewInstance() {

            InvocationContext context = new InvocationContextImpl();
            threadLocal.set(context);
            return context;
        }

        public static InvocationContext setCurrentInstance(InvocationContext context) {
            threadLocal.set(context);

            return context;
        }

        public static InvocationContext getCurrentInstance() {
            InvocationContext context = threadLocal.get();

            if (context == null) {
                context = createNewInstance();

                threadLocal.set(context);
            }

            return context;
        }

        public static void removeCurrentInstance() {
            threadLocal.remove();
        }
    }


}
