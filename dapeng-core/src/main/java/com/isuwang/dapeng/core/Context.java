package com.isuwang.dapeng.core;

/**
 * Context
 *
 * @author craneding
 * @date 16/3/14
 */
public class Context {

    private CodecProtocol codecProtocol = CodecProtocol.CompressedBinary;

    private String calleeIp;

    private int calleePort;

    private long calleeTimeout;

    private SoaHeader header;

    private Integer seqid;

    private boolean hasHeader = false;

    private int failedTimes = 0;

    private boolean isSoaTransactionProcess;

    private boolean isSoaGlobalTransactional;

    private Integer currentTransactionSequence = 0;

    private Integer currentTransactionId = 0;

    public int getFailedTimes() {
        return failedTimes;
    }

    public void setFailedTimes(int failedTimes) {
        this.failedTimes = failedTimes;
    }

    protected Context() {
    }

    public CodecProtocol getCodecProtocol() {
        return codecProtocol;
    }

    public void setCodecProtocol(CodecProtocol codecProtocol) {
        this.codecProtocol = codecProtocol;
    }

    public String getCalleeIp() {
        return calleeIp;
    }

    public void setCalleeIp(String calleeIp) {
        this.calleeIp = calleeIp;
    }

    public int getCalleePort() {
        return calleePort;
    }

    public void setCalleePort(int calleePort) {
        this.calleePort = calleePort;
    }

    public long getCalleeTimeout() {
        return calleeTimeout;
    }

    public void setCalleeTimeout(long calleeTimeout) {
        this.calleeTimeout = calleeTimeout;
    }

    public SoaHeader getHeader() {
        return header;
    }

    public void setHeader(SoaHeader header) {
        this.header = header;
    }

    public boolean isHasHeader() {
        return hasHeader;
    }

    public void setHasHeader(boolean hasHeader) {
        this.hasHeader = hasHeader;
    }

    public Integer getSeqid() {
        return seqid;
    }

    public void setSeqid(Integer seqid) {
        this.seqid = seqid;
    }

    public boolean isSoaTransactionProcess() {
        return isSoaTransactionProcess;
    }

    public void setSoaTransactionProcess(boolean soaTransactionProcess) {
        isSoaTransactionProcess = soaTransactionProcess;
    }

    public boolean isSoaGlobalTransactional() {
        return isSoaGlobalTransactional;
    }

    public void setSoaGlobalTransactional(boolean soaGlobalTransactional) {
        isSoaGlobalTransactional = soaGlobalTransactional;
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

    public static enum CodecProtocol {
        Binary((byte) 0), CompressedBinary((byte) 1), Json((byte) 2), Xml((byte) 3);

        private byte code;

        private CodecProtocol(byte code) {
            this.code = code;
        }

        public byte getCode() {
            return code;
        }

        public static CodecProtocol toCodecProtocol(byte code) {
            CodecProtocol[] values = CodecProtocol.values();
            for (CodecProtocol protocol : values) {
                if (protocol.getCode() == code)
                    return protocol;
            }

            return null;
        }
    }

}
