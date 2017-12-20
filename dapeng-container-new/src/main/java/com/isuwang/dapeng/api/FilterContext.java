package com.isuwang.dapeng.api;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface FilterContext {

    void setAttach(Filter filter, String key, Object value);

    Object getAttach(Filter filter, String key);


}
