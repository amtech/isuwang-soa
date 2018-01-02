package com.isuwang.dapeng.core;

import com.isuwang.dapeng.core.enums.CodecProtocol;

import java.util.Optional;

/**
 * Created by lihuimin on 2017/12/21.
 */
public interface InvocationContext {

    void setCodecProtocol(CodecProtocol protocol);

    CodecProtocol getCodecProtocol();

    Optional<String> getCalleeIp();

    void setCalleeIp(Optional<String> calleeIp);

    Optional<Integer> getCalleePort();

    void setCalleePort(Optional<Integer> calleePort);

    public Optional<Integer> getTransactionId();

    public void setTransactionId(Optional<Integer> transactionId);

    void setLastInfo(InvocationInfo invocationInfo);

    InvocationInfo getLastInfo();


    // seqid
    // tid
    interface InvocationInfo {
    }

//    interface Set {
//        // codecProtocol
//        // calleeIp, calleePort
//        // loadbalance
//        // timeout
//        // sessionid
//        // cookie
//        // uid
//        // staffid
//    }




    /*
        InvocationContext context = InvocationContextFactory.getInvocationContext();

        context.setCalleeIp("....");
        context.setTimeout(10s);

        someclient.somethod();

        context.getLastInfo().getCalleeIp();
        context.getLastInfo().getTid();

     */

}
