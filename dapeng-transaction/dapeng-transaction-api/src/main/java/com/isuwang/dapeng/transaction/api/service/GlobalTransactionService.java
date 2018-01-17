
package com.isuwang.dapeng.transaction.api.service;

import com.isuwang.dapeng.core.Processor;
import com.isuwang.dapeng.core.Service;
import com.isuwang.dapeng.core.SoaException;
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransaction;
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionsStatus;

/**
 *
 **/
@Service(name="com.isuwang.dapeng.transaction.api.service.GlobalTransactionService" ,version = "1.0.0")
@Processor(className = "com.isuwang.dapeng.transaction.api.GlobalTransactionServiceCodec$Processor")
public interface GlobalTransactionService {

    /**
     *
     **/
    TGlobalTransaction create(TGlobalTransaction globalTransaction) throws SoaException;

    /**
     *
     **/
    void update(Integer globalTransactionId, Integer currSequence, TGlobalTransactionsStatus status) throws SoaException;

}
        