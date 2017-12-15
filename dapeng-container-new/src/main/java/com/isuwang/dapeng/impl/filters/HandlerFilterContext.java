package com.isuwang.dapeng.impl.filters;

import com.isuwang.dapeng.api.filters.FilterContext;
import com.isuwang.dapeng.api.filters.HandlerFilter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class HandlerFilterContext implements FilterContext {

    private Map<String, Object> attachments;
    @Override
    public void setAttach(HandlerFilter filter, String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(key, value);
    }

    @Override
    public Object getAttach(HandlerFilter filter, String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }


}
