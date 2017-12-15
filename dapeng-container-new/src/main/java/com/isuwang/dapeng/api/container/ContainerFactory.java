package com.isuwang.dapeng.api.container;


public class ContainerFactory {

    private static Container applicationContainer;

    public static void initContainer(Container container) {
        applicationContainer = container;
    }

    public static Container getContainer() {
        return applicationContainer;
    }
}
