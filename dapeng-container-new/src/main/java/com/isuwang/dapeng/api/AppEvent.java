package com.isuwang.dapeng.api;

import java.util.EventObject;

public class AppEvent extends EventObject {

    private AppEventType eventType;

    public AppEvent(Application application, AppEventType eventType) {
        super(application);
        this.eventType = eventType;
    }

    public AppEventType getEventType() {
        return this.eventType;
    }
}
