package com.isuwang.dapeng.impl.container;

import com.isuwang.dapeng.api.Container;
import com.isuwang.dapeng.api.ContainerFactorySpi;

import java.util.List;

public class DapengContainerFactorySpiml implements ContainerFactorySpi {
    @Override
    public Container createInstance(List<ClassLoader> applicationCls) {
        return new DapengContainer(applicationCls);
    }
}
