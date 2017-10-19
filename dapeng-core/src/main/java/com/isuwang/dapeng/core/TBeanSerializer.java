package com.isuwang.dapeng.core;

import com.isuwang.org.apache.thrift.TException;
import com.isuwang.org.apache.thrift.protocol.TProtocol;

/**
 * 序列化接口
 *
 * @author craneding
 * @date 15/4/26
 */
@Deprecated
public interface TBeanSerializer<T> {

    void read(T bean, TProtocol iprot) throws TException;

    void write(T bean, TProtocol oprot) throws TException;

    void validate(T bean) throws TException;

    String toString(T bean);
}
