package com.isuwang.dapeng.api;


public class ContainerFactory {

    private static Container applicationContainer;

//    public static DapengContainer initDapengContainer() {
//        if (applicationContainer == null) {
//            applicationContainer = new DapengContainer();
//        }
//        return (DapengContainer) applicationContainer;
//    }

    public static void initDapengContainer(Container container) {
        applicationContainer = container;
    }

    public static Container getContainer() {
        return applicationContainer;
    }
}
