package com.isuwang.dapeng.transaction.db.action

import com.isuwang.dapeng.transaction.TransactionDB._
import com.isuwang.dapeng.transaction.api.domain.{TGlobalTransactionProcess, TGlobalTransactionProcessStatus}
import com.isuwang.dapeng.transaction.db.domain.GlobalTransactionProcess
import com.isuwang.dapeng.transaction.support.Action
import com.isuwang.dapeng.transaction.support.Assert._
import com.isuwang.dapeng.transaction.support.Implicit._
import com.isuwang.dapeng.transaction.utils.ErrorCode
import wangzx.scala_commons.sql._

import scala.collection.JavaConversions._

/**
  * 查找所有的成功的或者未知的事务过程记录
  * 降序
  *
  * Created by tangliu on 2016/4/13.
  */
class GlobalTransactionProcessFindAction(transactionId: Int) extends Action[java.util.List[TGlobalTransactionProcess]] {

  override def inputCheck: Unit = {

    assert(transactionId > 0, ErrorCode.INPUTERROR.getCode, "全局事务id错误")
  }

  override def action: java.util.List[TGlobalTransactionProcess] = {

    val selectSql =
      sql"""
         SELECT *
         FROM global_transaction_process
         WHERE transaction_id = ${transactionId} and (status = ${TGlobalTransactionProcessStatus.Success.getValue()} OR status = ${TGlobalTransactionProcessStatus.Unknown.getValue()})
         ORDER BY transaction_sequence DESC
       """
    rows[GlobalTransactionProcess](selectSql).toThrifts[TGlobalTransactionProcess]

  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}


/**
  * 查找所有的失败的或者未知的事务过程记录
  * 升序
  *
  * Created by tangliu on 2016/5/5.
  */
class FailedTransactionProcessFindAction(transactionId: Int) extends Action[java.util.List[TGlobalTransactionProcess]] {

  override def inputCheck: Unit = {

    assert(transactionId > 0, ErrorCode.INPUTERROR.getCode, "全局事务id错误")
  }

  override def action: java.util.List[TGlobalTransactionProcess] = {

    val selectSql =
      sql"""
         SELECT *
         FROM global_transaction_process
         WHERE transaction_id = ${transactionId} and (status = ${TGlobalTransactionProcessStatus.Fail.getValue()} OR status = ${TGlobalTransactionProcessStatus.Unknown.getValue()})
         ORDER BY transaction_sequence ASC
       """
    rows[GlobalTransactionProcess](selectSql).toThrifts[TGlobalTransactionProcess]

  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}
