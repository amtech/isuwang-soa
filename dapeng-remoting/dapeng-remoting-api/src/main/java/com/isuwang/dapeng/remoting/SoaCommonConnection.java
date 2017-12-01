package com.isuwang.dapeng.remoting;

import com.isuwang.dapeng.core.TCommonBeanSerializer;
import com.isuwang.org.apache.thrift.TException;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaCommonConnection {

    <REQ, RESP> RESP send(REQ request, TCommonBeanSerializer<REQ> requestSerializer, TCommonBeanSerializer<RESP> responseSerializer,boolean isOldVersion) throws TException;

    <REQ, RESP> Future<RESP> sendAsync(REQ request, TCommonBeanSerializer<REQ> requestSerializer, TCommonBeanSerializer<RESP> responseSerializer, long timeout) throws TException;

}
