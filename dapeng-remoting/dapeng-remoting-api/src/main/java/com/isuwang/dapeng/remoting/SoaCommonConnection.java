package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.BeanSerializer;
import com.isuwang.org.apache.thrift.TException;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaCommonConnection {

    <REQ, RESP> RESP send(REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer) throws TException;

    <REQ, RESP> Future<RESP> sendAsync(REQ request, BeanSerializer<REQ> requestSerializer, BeanSerializer<RESP> responseSerializer, long timeout) throws TException;

}
