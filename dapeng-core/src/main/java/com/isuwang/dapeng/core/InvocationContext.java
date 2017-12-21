package com.isuwang.dapeng.core;

import com.isuwang.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * 客户端上下文
 *
 * @author craneding
 * @date 15/9/24
 */
public class InvocationContext{

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private SoaHeader header;

    private Integer seqid;

    private int failedTimes = 0;

    private boolean isSoaTransactionProcess;

    private Integer currentTransactionSequence = 0;

    private Integer currentTransactionId = 0;

    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public void setHeader(SoaHeader header) {
        this.header = header;
    }

    public Integer getSeqid() {
        return seqid;
    }

    public void setSeqid(Integer seqid) {
        this.seqid = seqid;
    }

    public int getFailedTimes() {
        return failedTimes;
    }

    public void setFailedTimes(int failedTimes) {
        this.failedTimes = failedTimes;
    }

    public boolean isSoaTransactionProcess() {
        return isSoaTransactionProcess;
    }

    public void setSoaTransactionProcess(boolean soaTransactionProcess) {
        isSoaTransactionProcess = soaTransactionProcess;
    }

    public Integer getCurrentTransactionSequence() {
        return currentTransactionSequence;
    }

    public void setCurrentTransactionSequence(Integer currentTransactionSequence) {
        this.currentTransactionSequence = currentTransactionSequence;
    }

    public Integer getCurrentTransactionId() {
        return currentTransactionId;
    }

    public void setCurrentTransactionId(Integer currentTransactionId) {
        this.currentTransactionId = currentTransactionId;
    }

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

        public static InvocationContext createNewInstance() {

            InvocationContext context = new InvocationContext();
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
