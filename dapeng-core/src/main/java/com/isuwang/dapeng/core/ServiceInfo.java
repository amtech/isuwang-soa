package com.isuwang.dapeng.core;

public class ServiceInfo {

    private String serviceName;
    private String version;
    private String serviceType; //task, commonService. cron..etc.

    /**
     * 用于Task 拿到对应的 class 类型
     * 方便查找 对应类型的信息
     */
    Class<?> ifaceClass;


    public Class<?> getIfaceClass() {
        return ifaceClass;
    }

    public void setIfaceClass(Class<?> ifaceClass) {
        this.ifaceClass = ifaceClass;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
