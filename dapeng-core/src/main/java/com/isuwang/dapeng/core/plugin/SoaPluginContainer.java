package com.isuwang.dapeng.core.plugin;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by admin on 2017/10/16.
 */
public interface SoaPluginContainer {

    public static Map<Object, Class<?>> contexts=new HashMap<>();


    public void start() ;

}
