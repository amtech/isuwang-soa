package com.isuwang.dapeng.transaction.db.action

import java.sql.Timestamp
import java.util.Date

import com.isuwang.dapeng.core.SoaException
import com.isuwang.dapeng.transaction.TransactionDB._
import com.isuwang.dapeng.transaction.TransactionSQL
import com.isuwang.dapeng.transaction.api.domain.{TGlobalTransactionProcessExpectedStatus, TGlobalTransactionProcessStatus}
import com.isuwang.dapeng.transaction.support.Action
import com.isuwang.dapeng.transaction.support.Assert._
import com.isuwang.dapeng.transaction.utils.ErrorCode
import org.slf4j.{Logger, LoggerFactory}
import wangzx.scala_commons.sql._


/**
  * Created by tangliu on 2016/4/12.
  */
class GlobalTransactionProcessUpdateAction(processId: Int, responseJson: String, status: TGlobalTransactionProcessStatus) extends Action[Unit] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GlobalTransactionProcessUpdateAction])

  override def inputCheck: Unit = {
    assert(processId > 0, ErrorCode.INPUTERROR.getCode, "globalTransactionProcess.id 错误")
  }

  override def action: Unit = {

    val processOpt = TransactionSQL.getTransactionProcessForUpdate(processId)

    val now: Date = new Date
    val updatedAt = new Timestamp(now.getTime)

    if (!processOpt.isDefined)
      throw new SoaException(ErrorCode.NOTEXIST.getCode, ErrorCode.NOTEXIST.getMsg)
    else {

      val process = processOpt.get

      LOGGER.info("更新事务过程({})前,状态({}),过程响应参数({})", process.id.toString, process.status.toString, process.responseJson);

      esql(
        sql"""
                update global_transaction_process
                set
                  status = ${status.getValue},
                  response_json = ${responseJson},
                  updated_at = ${updatedAt}
                where id = ${processId}
            """
      )

      LOGGER.info("更新事务过程({})后,状态({}),过程响应参数({})", process.id.toString, status.getValue.toString, responseJson);
    }

  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}

/**
  * 更新期望状态
  *
  * @param processId
  * @param status
  */
class GlobalTransactionProcessExpectedStatusUpdateAction(processId: Int, status: TGlobalTransactionProcessExpectedStatus) extends Action[Unit] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GlobalTransactionProcessExpectedStatusUpdateAction])

  override def inputCheck: Unit = {
    assert(processId > 0, ErrorCode.INPUTERROR.getCode, "globalTransactionProcess.id 错误")
  }

  override def action: Unit = {

    val now: Date = new Date
    val updated_at = new Timestamp(now.getTime)

    val processOpt = TransactionSQL.getTransactionProcessForUpdate(processId)
    if (!processOpt.isDefined)
      throw new SoaException(ErrorCode.NOTEXIST.getCode, ErrorCode.NOTEXIST.getMsg)
    else {

      val process = processOpt.get

      LOGGER.info("更新事务过程({})前,过程目标状态({})", process.id, process.expectedStatus)

      esql(
        sql"""
                update global_transaction_process
                set
                  expected_status = ${status.getValue},
                  updated_at = ${updated_at}
                where id = ${processId}
            """
      )

      LOGGER.info("更新事务过程({})后,过程目标状态({})", process.id, status.getValue)
    }

  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}


/**
  * 更新重试次数和下次重试时间
  *
  * @param processId
  */
class GlobalTransactionProcessUpdateAfterRollbackFail(processId: Int) extends Action[Unit] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GlobalTransactionProcessUpdateAfterRollbackFail])

  override def inputCheck: Unit = {
    assert(processId > 0, ErrorCode.INPUTERROR.getCode, "globalTransactionProcess.id 错误")
  }

  override def action: Unit = {

    val processOpt = TransactionSQL.getTransactionProcessForUpdate(processId)

    val now: Date = new Date
    val updated_at = new Timestamp(now.getTime)

    if (!processOpt.isDefined)
      throw new SoaException(ErrorCode.NOTEXIST.getCode, ErrorCode.NOTEXIST.getMsg)
    else {

      val process = processOpt.get

      LOGGER.info("更新事务过程({})前,重试次数({}),下次重试时间({})", process.id.toString, process.redoTimes.toString, new Date(process.nextRedoTime.getTime).toString)

      process.redoTimes += 1
      process.nextRedoTime = new Timestamp(new Date().getTime + (30 * 1000))

      esql(
        sql"""
                update global_transaction_process
                set
                  redo_times = ${process.redoTimes},
                  next_redo_time = ${process.nextRedoTime},
                  updated_at = ${updated_at}
                where id = ${processId}
            """
      )

      LOGGER.info("更新事务过程({})前,重试次数({}),下次重试时间({})", process.id.toString, process.redoTimes.toString, new Date(process.nextRedoTime.getTime).toString)
    }

  }

  override def postCheck: Unit = {}

  override def preCheck: Unit = {}
}
