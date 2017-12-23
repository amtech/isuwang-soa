package com.isuwang.dapeng.core;

import com.isuwang.org.apache.thrift.TException;

import java.util.concurrent.Future;

/**
 * @author craneding
 * @date 16/3/1
 */
public interface SoaConnection {

    <REQ, RESP> RESP send(
            String service,
            String version,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer) throws Exception;

    <REQ, RESP> Future<RESP> sendAsync(
            String service,
            String version,
            String method,
            REQ request,
            BeanSerializer<REQ> requestSerializer,
            BeanSerializer<RESP> responseSerializer,
            long timeout) throws Exception;
}
