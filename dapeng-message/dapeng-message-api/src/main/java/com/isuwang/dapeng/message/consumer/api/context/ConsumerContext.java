package com.isuwang.dapeng.message.consumer.api.context;

import com.isuwang.dapeng.core.SoaProcessFunction;
import com.isuwang.dapeng.core.TBeanSerializer;
import com.isuwang.dapeng.core.message.MessageConsumer;
import com.isuwang.dapeng.core.message.MessageConsumerAction;


/**
 * Created by tangliu on 2016/8/4.
 */
public class ConsumerContext {

    public Object iface;

    public SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>> soaProcessFunction;

    public Object getIface() {
        return iface;
    }

    public void setIface(Object iface) {
        this.iface = iface;
    }

    public SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>> getSoaProcessFunction() {
        return soaProcessFunction;
    }

    public void setSoaProcessFunction(SoaProcessFunction<Object, Object, Object, ? extends TBeanSerializer<Object>, ? extends TBeanSerializer<Object>> soaProcessFunction) {
        this.soaProcessFunction = soaProcessFunction;
    }

    MessageConsumerAction action;
    MessageConsumer consumer;

    public MessageConsumerAction getAction() {
        return action;
    }

    public void setAction(MessageConsumerAction action) {
        this.action = action;
    }

    public MessageConsumer getConsumer() {
        return consumer;
    }

    public void setConsumer(MessageConsumer consumer) {
        this.consumer = consumer;
    }
}
