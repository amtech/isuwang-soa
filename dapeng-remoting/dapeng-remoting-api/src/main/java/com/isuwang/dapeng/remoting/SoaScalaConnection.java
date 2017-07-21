package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.TScalaBeanSerializer;
import com.isuwang.org.apache.thrift.TException;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaScalaConnection {

    <REQ, RESP> RESP send(REQ request, TScalaBeanSerializer<REQ> requestSerializer, TScalaBeanSerializer<RESP> responseSerializer) throws TException;

    <REQ, RESP> Future<RESP> sendAsync(REQ request, TScalaBeanSerializer<REQ> requestSerializer, TScalaBeanSerializer<RESP> responseSerializer, long timeout) throws TException;

}
