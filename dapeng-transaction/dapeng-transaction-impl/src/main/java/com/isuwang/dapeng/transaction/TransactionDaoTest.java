package com.isuwang.dapeng.transaction;

import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.transaction.api.domain.*;
import com.isuwang.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.isuwang.dapeng.transaction.api.service.GlobalTransactionService;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Date;

/**
 * Created by tangliu on 17/7/30.
 */
public class TransactionDaoTest {


    private static ApplicationContext ctx = null;

    @org.junit.BeforeClass
    public static void onlyOnce() {
        ctx = new ClassPathXmlApplicationContext("META-INF/spring/services.xml");
    }

    @Test
    public void testGlobalTransactionSave() throws SoaException {
        GlobalTransactionService service = ctx.getBean("globalTransactionService", GlobalTransactionService.class);

        TGlobalTransaction transaction = new TGlobalTransaction();
        transaction.setUpdatedBy(101);
        transaction.setCurrSequence(12);
        transaction.setCreatedBy(101);
        transaction.setStatus(TGlobalTransactionsStatus.New);
        transaction.setCreatedAt(new Date());

        System.out.println(service.create(transaction));
    }

    @Test
    public void testGlobalTransactionUpdate() throws SoaException {
        GlobalTransactionService service = ctx.getBean("globalTransactionService", GlobalTransactionService.class);

        service.update(100252, 9, TGlobalTransactionsStatus.HasRollback);
    }


    @Test
    public void testGlobalTransactionProcessSave() throws SoaException {
        GlobalTransactionProcessService service = ctx.getBean("globalTransactionProcessService", GlobalTransactionProcessService.class);

        TGlobalTransactionProcess transactionProcess = new TGlobalTransactionProcess();

        transactionProcess.setCreatedAt(new Date());
        transactionProcess.setCreatedBy(0);
        transactionProcess.setExpectedStatus(TGlobalTransactionProcessExpectedStatus.Success);

        transactionProcess.setServiceName("HelloService");
        transactionProcess.setMethodName("sayHello");
        transactionProcess.setVersionName("1.0.0");
        transactionProcess.setRollbackMethodName("sayHello" + "_rollback");

        transactionProcess.setRequestJson("");
        transactionProcess.setResponseJson("");

        transactionProcess.setStatus(TGlobalTransactionProcessStatus.New);
        transactionProcess.setTransactionId(100252);
        transactionProcess.setTransactionSequence(0);

        transactionProcess.setRedoTimes(0);
        transactionProcess.setNextRedoTime(new Date(new Date().getTime() + 30 * 1000));

        System.out.println(service.create(transactionProcess));
    }

    @Test
    public void testUpdateGlobalTransactionProcess() throws SoaException {
        GlobalTransactionProcessService service = ctx.getBean("globalTransactionProcessService", GlobalTransactionProcessService.class);
        service.update(70173, "{success:\"\"}", TGlobalTransactionProcessStatus.Success);
    }

    @Test
    public void testUpdateGlobalTransactionProcessRedoTimes() throws SoaException {
        GlobalTransactionProcessService service = ctx.getBean("globalTransactionProcessService", GlobalTransactionProcessService.class);
        service.updateRedoTimes(70173);
    }


//    @Test
//    public void testSaveThrowException() throws Exception{
//        IUserService service=ctx.getBean("userService",IUserService.class);
//        service.saveUserThrowException();
//    }
//
//    @Test
//    public void testJDBCDaoQuery(){
//        IUserService service=ctx.getBean("userService",IUserService.class);
//        service.findUsers();
//    }
}
