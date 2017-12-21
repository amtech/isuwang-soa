package com.isuwang.dapeng.impl.filters;


import com.isuwang.dapeng.api.FilterContext;
import com.isuwang.dapeng.api.Filter;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lihuimin on 2017/12/11.
 */
public class HandlerFilterContext implements FilterContext {

//    private Map<String, Object> attachments;

    private Map<Filter, Map<String, Object>> attachments = new HashMap<>();

    @Override
    public void setAttach(Filter filter, String key, Object value) {
        Map<String, Object> attches = attachments.get(filter);
        if(attches == null){
            attches = new HashMap<>();
            attachments.put(filter, attches);
        }
        attches.put(key, value);
    }

    @Override
    public Object getAttach(Filter filter, String key) {
        Map<String, Object> attaches = attachments.get(filter);
        if(attaches != null)
            return attaches.get(key);
        else return null;
    }


}
