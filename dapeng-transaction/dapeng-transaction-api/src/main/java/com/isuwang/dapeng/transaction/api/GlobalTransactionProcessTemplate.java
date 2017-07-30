package com.isuwang.dapeng.transaction.api;

import com.google.gson.Gson;
import com.isuwang.dapeng.core.InvocationContext;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.core.TransactionContext;
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionProcess;
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionProcessStatus;
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionProcessExpectedStatus;
import com.isuwang.dapeng.transaction.api.service.GlobalTransactionProcessService;
import com.isuwang.org.apache.thrift.TException;

import java.util.Date;
import java.util.Optional;

/**
 * Soa Transactional Process Template
 *
 * @author craneding
 * @date 16/4/11
 */
public class GlobalTransactionProcessTemplate<REQ> {

    private REQ req;

    public GlobalTransactionProcessTemplate(REQ req) {
        this.req = req;
    }

    public <T> T execute(GlobalTransactionCallback<T> action) throws TException {
        final GlobalTransactionProcessService service = GlobalTransactionFactory.getGlobalTransactionProcessService();

        TGlobalTransactionProcess transactionProcess = null;

        boolean success = false, unknown = false;
        T result = null;

        try {
            InvocationContext invocationContext = InvocationContext.Factory.getCurrentInstance();
            TransactionContext transactionContext = TransactionContext.Factory.getCurrentInstance();

            transactionContext.setCurrentTransactionSequence(transactionContext.getCurrentTransactionSequence() + 1);

            invocationContext.getHeader().setTransactionId(Optional.of(transactionContext.getCurrentTransactionId()));
            invocationContext.getHeader().setTransactionSequence(Optional.of(transactionContext.getCurrentTransactionSequence()));

            transactionProcess = new TGlobalTransactionProcess();
            transactionProcess.setCreatedAt(new Date());
            transactionProcess.setCreatedBy(0);
            transactionProcess.setExpectedStatus(TGlobalTransactionProcessExpectedStatus.Success);

            transactionProcess.setServiceName(invocationContext.getHeader().getServiceName());
            transactionProcess.setMethodName(invocationContext.getHeader().getMethodName());
            transactionProcess.setVersionName(invocationContext.getHeader().getVersionName());
            transactionProcess.setRollbackMethodName(invocationContext.getHeader().getMethodName() + "_rollback");

            transactionProcess.setRequestJson(req == null ? "" : new Gson().toJson(req));
            transactionProcess.setResponseJson("");

            transactionProcess.setStatus(TGlobalTransactionProcessStatus.New);
            transactionProcess.setTransactionId(transactionContext.getCurrentTransactionId());
            transactionProcess.setTransactionSequence(transactionContext.getCurrentTransactionSequence());

            transactionProcess.setRedoTimes(0);
            transactionProcess.setNextRedoTime(new Date(new Date().getTime() + 30 * 1000));

            transactionProcess = service.create(transactionProcess);

            result = action.doInTransaction();

            success = true;

            return result;
        } catch (SoaException e) {
            switch (e.getCode()) {
                case "AA98":// 连接失败
                    unknown = false;
                    break;
                case "AA96":// 超时
                case "9999":// 未知
                    unknown = true;
                    break;
                default:// 明确错误
                    unknown = false;
                    break;
            }

            throw e;
        } finally {
            final TGlobalTransactionProcessStatus status = success ? TGlobalTransactionProcessStatus.Success : (unknown ? TGlobalTransactionProcessStatus.Unknown : TGlobalTransactionProcessStatus.Fail);

            if (transactionProcess.getId() != null) {
                service.update(transactionProcess.getId(), result == null ? "" : new Gson().toJson(result), status);
            }
        }
    }

}
