package com.isuwang.dapeng.transaction.db.action

import java.sql.Timestamp
import java.util.Date

import com.isuwang.dapeng.core.SoaException
import com.isuwang.dapeng.transaction.TransactionDB._
import com.isuwang.dapeng.transaction.TransactionSQL
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionsStatus
import com.isuwang.dapeng.transaction.support.Action
import com.isuwang.dapeng.transaction.support.Assert._
import com.isuwang.dapeng.transaction.utils.ErrorCode
import org.slf4j.{Logger, LoggerFactory}
import wangzx.scala_commons.sql._


/**
  * Created by tangliu on 2016/4/12.
  */
class GlobalTransactionUpdateAction(transactionId: Int, currSequence: Int, status: TGlobalTransactionsStatus) extends Action[Unit] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GlobalTransactionUpdateAction])

  /**
    * 输入检查：查询、新增、更新、删除等输入条件
    */
  override def inputCheck: Unit = {
    assert(transactionId > 0, ErrorCode.INPUTERROR.getCode, "transactionId 错误")
  }

  /**
    * 动作
    */
  override def action: Unit = {

    val transactionOpt = TransactionSQL.getTransactionForUpdate(transactionId)

    val now: Date = new Date
    val updatedAt = new Timestamp(now.getTime)

    if (!transactionOpt.isDefined)
      throw new SoaException(ErrorCode.NOTEXIST.getCode, ErrorCode.NOTEXIST.getMsg)
    else {

      val globalTransaction = transactionOpt.get

      LOGGER.info("更新全局事务({})前,状态({}),当前过程序列号({})", globalTransaction.id.toString, globalTransaction.status.toString, globalTransaction.currSequence.toString);

      esql(
        sql"""
                update global_transactions
                set
                  status = ${status.getValue},
                  curr_sequence = ${currSequence},
                  updated_at = ${updatedAt}
                where id = ${transactionId}
            """
      )

      LOGGER.info("更新全局事务({})后,状态({}),当前过程序列号({})", transactionId.toString, status.getValue.toString, currSequence.toString);
    }

  }

  /**
    * 后置条件检查
    */
  override def postCheck: Unit = {}

  /**
    * 前置条件检查：动作、状态等业务逻辑
    */
  override def preCheck: Unit = {}
}
