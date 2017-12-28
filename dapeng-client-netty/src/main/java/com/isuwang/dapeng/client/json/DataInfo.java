package com.isuwang.dapeng.client.json;


import com.isuwang.dapeng.core.metadata.Method;
import com.isuwang.dapeng.core.metadata.Service;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据信息
 *
 * @author craneding
 * @date 15/4/26
 */
public class DataInfo implements Serializable {
    private static final long serialVersionUID = 2968518564011478972L;

    private static final AtomicInteger _seqid = new AtomicInteger(0);

    private transient final int seqid = _seqid.incrementAndGet();

    /**
     * 服务名，包括命名空间
     */
    private String serviceName;

    /**
     * 方法名
     */
    private String methodName;

    /**
     * 版本号
     */
    private String version;

    /**
     * Consumes数据
     */
    private String consumesValue;

    /**
     * Consums数据格式
     */
    private String consumesType;

    /**
     * 原服务
     */
    private Service service;

    /**
     * 匹配到的方法
     */
    private Method method;

    /**
     * 头信息
     */
    private Map<String, Object> headers;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getConsumesValue() {
        return consumesValue;
    }

    public void setConsumesValue(String consumesValue) {
        this.consumesValue = consumesValue;
    }

    public String getConsumesType() {
        return consumesType;
    }

    public void setConsumesType(String consumesType) {
        this.consumesType = consumesType;
    }

    public Service getService() {
        return service;
    }

    public void setService(Service service) {
        this.service = service;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public int getSeqid() {
        return seqid;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, Object> headers) {
        this.headers = headers;
    }
}
