package com.isuwang.dapeng.api;

import java.util.List;

public interface ContainerFactorySpi {
    Container createInstance(List<ClassLoader> applicationCls);
}
