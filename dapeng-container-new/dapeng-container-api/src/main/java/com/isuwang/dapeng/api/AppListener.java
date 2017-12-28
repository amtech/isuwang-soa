package com.isuwang.dapeng.api;


import com.isuwang.dapeng.api.events.AppEvent;

import java.util.EventListener;

public interface AppListener extends EventListener {

    public void appRegistered(AppEvent event);

    public void appUnRegistered(AppEvent event);
}
