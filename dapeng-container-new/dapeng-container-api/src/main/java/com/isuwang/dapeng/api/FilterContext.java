package com.isuwang.dapeng.api;

/**
 * Created by lihuimin on 2017/12/11.
 */
public interface FilterContext {

    void setAttach(HandlerFilter filter, String key, Object value);

    Object getAttach(HandlerFilter filter, String key);


}
