package com.isuwang.dapeng.transaction.db.action

import com.isuwang.dapeng.transaction.TransactionDB._
import com.isuwang.dapeng.transaction.TransactionSQL
import com.isuwang.dapeng.transaction.api.domain.{TGlobalTransaction, TGlobalTransactionProcessStatus, TGlobalTransactionsStatus}
import com.isuwang.dapeng.transaction.db.domain.GlobalTransaction
import com.isuwang.dapeng.transaction.support.Action
import com.isuwang.dapeng.transaction.support.Assert._
import com.isuwang.dapeng.transaction.support.Implicit._
import com.isuwang.dapeng.transaction.utils.ErrorCode
import wangzx.scala_commons.sql._

import scala.collection.JavaConversions._

/**
  * 查找所有的失败或部分回滚的全局事务记录
  *
  *
  * Created by tangliu on 2016/4/13.
  */
class GlobalTransactionsFindAction() extends Action[java.util.List[TGlobalTransaction]] {

  override def inputCheck: Unit = {}

  override def action: java.util.List[TGlobalTransaction] = {
    val selectSql =
      sql"""
         SELECT id, status, curr_sequence
         FROM global_transactions
         WHERE status = ${TGlobalTransactionsStatus.Fail.getValue()} or status = ${TGlobalTransactionsStatus.PartiallyRollback.getValue()}
       """
    rows[GlobalTransaction](selectSql).toThrifts[TGlobalTransaction]
  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}


/**
  * 查找所有状态为成功，但子过程中有失败的全局事务记录
  */
class GlobalSucTransactionWithFailProcessFindAction() extends Action[java.util.List[TGlobalTransaction]] {

  override def inputCheck: Unit = {}

  override def action: java.util.List[TGlobalTransaction] = {
    val selectSql =
      sql"""
         SELECT g.id, g.status, g.curr_sequence
         FROM global_transactions g INNER JOIN global_transaction_process p ON g.id = p.transaction_id
         WHERE g.status = ${TGlobalTransactionsStatus.Success.getValue()} AND (p.status = ${TGlobalTransactionProcessStatus.Fail.getValue()} OR p.status = ${TGlobalTransactionProcessStatus.Unknown.getValue()})
         GROUP BY g.id
       """
    rows[GlobalTransaction](selectSql).toThrifts[TGlobalTransaction]
  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}


/**
  * 根据id查询全局事务记录
  */
class GlobalTransactionFindByIdAction(transactionId: Int) extends Action[TGlobalTransaction] {

  override def inputCheck: Unit = {
    assert(transactionId > 0, ErrorCode.INPUTERROR.getCode, "全局事务id错误")
  }

  override def action: TGlobalTransaction = {

    val transactionOpt = TransactionSQL.getTransactionForUpdate(transactionId)

    if (transactionOpt.isDefined)
      transactionOpt.get.toThrift[TGlobalTransaction]
    else
      null
  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}
