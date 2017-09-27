package com.isuwang.dapeng.message.consumer.api.context;

import com.isuwang.dapeng.core.SoaProcessFunction;
import com.isuwang.dapeng.core.TCommonBeanSerializer;


/**
 * Created by tangliu on 2016/8/4.
 */
public class ConsumerContext {

    public Object iface;

    public SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction;

    public Object getIface() {
        return iface;
    }

    public void setIface(Object iface) {
        this.iface = iface;
    }

    public SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> getSoaProcessFunction() {
        return soaProcessFunction;
    }

    public void setSoaProcessFunction(SoaProcessFunction<Object, Object, Object, ? extends TCommonBeanSerializer<Object>, ? extends TCommonBeanSerializer<Object>> soaProcessFunction) {
        this.soaProcessFunction = soaProcessFunction;
    }

    Object action;
    Object consumer;

    public Object getAction() {
        return action;
    }

    public void setAction(Object action) {
        this.action = action;
    }

    public Object getConsumer() {
        return consumer;
    }

    public void setConsumer(Object consumer) {
        this.consumer = consumer;
    }

    String groupId;

    String topic;

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
