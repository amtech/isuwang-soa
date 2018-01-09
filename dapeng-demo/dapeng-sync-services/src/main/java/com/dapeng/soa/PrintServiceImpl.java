package com.dapeng.soa;

import com.isuwang.dapeng.core.SoaException;
import com.isuwang.soa.account.enums.AccountType;
import com.isuwang.soa.service.PrintService;
/**
 * Created by admin on 2017/8/16.
 */

public class PrintServiceImpl implements PrintService {

    @Override
    public String printInfo(com.isuwang.soa.info.Info info) throws SoaException {

//        StaffServiceClient staffServiceClient = new StaffServiceClient();
//        TStaffEx staffEx=staffServiceClient.getById(16218);
//        return "say:"+info.code+" staff:"+staffEx.getName();
        return "say:"+info.code+" methodName : printInfo";

    }

    @Override
    public String printInfo2(String name) throws SoaException {

        System.out.println("Receiver String Message : " + name);
        return "hello,"+name;

    }

    @Override
    public void print(){
        System.out.println("test void method");
    }


    @Override
    public String printInfo3(AccountType var1){
        return "test";
    }


}
