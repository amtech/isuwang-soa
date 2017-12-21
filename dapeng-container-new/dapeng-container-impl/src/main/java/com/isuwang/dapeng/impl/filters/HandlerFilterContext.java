package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.api.FilterContext;
import com.isuwang.dapeng.api.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class HandlerFilterContext implements FilterContext {

    private Map<String, Object> attachments;
    @Override
    public void setAttach(Filter filter, String key, Object value) {
        if (attachments == null) {
            attachments = new HashMap<String, Object>();
        }
        attachments.put(key, value);
    }

    @Override
    public Object getAttach(Filter filter, String key) {
        if (attachments == null) {
            return null;
        }
        return attachments.get(key);
    }


}
