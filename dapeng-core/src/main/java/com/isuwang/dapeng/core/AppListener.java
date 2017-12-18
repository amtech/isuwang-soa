package com.isuwang.dapeng.core;


import com.isuwang.dapeng.core.events.AppEvent;

import java.util.EventListener;

public interface AppListener extends EventListener {

    public void appRegistered(AppEvent event);

    public void appUnRegistered(AppEvent event);
}
