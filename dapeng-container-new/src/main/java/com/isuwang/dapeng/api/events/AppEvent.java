package com.isuwang.dapeng.api.events;

import com.isuwang.dapeng.api.container.Application;

import java.util.EventObject;

public class AppEvent extends EventObject {

    private Application application;

    public AppEvent(Application application) {
        super(application);
        this.application = application;
    }
}
