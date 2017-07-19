package com.isuwang.dapeng.transaction.db.action

import java.sql.Timestamp
import java.util.Date

import com.isuwang.dapeng.transaction.TransactionSQL
import com.isuwang.dapeng.transaction.api.domain.TGlobalTransactionProcess
import com.isuwang.dapeng.transaction.db.domain.GlobalTransactionProcess
import com.isuwang.dapeng.transaction.support.Action
import com.isuwang.dapeng.transaction.support.Assert._
import com.isuwang.dapeng.transaction.utils.ErrorCode
import org.slf4j.{Logger, LoggerFactory}

/**
  * Created by tangliu on 2016/4/12.
  */
class GlobalTransactionProcessCreateAction(dto: TGlobalTransactionProcess) extends Action[TGlobalTransactionProcess] {

  val LOGGER: Logger = LoggerFactory.getLogger(classOf[GlobalTransactionProcessCreateAction])

  /**
    * 输入检查：查询、新增、更新、删除等输入条件
    */
  override def inputCheck: Unit = {
    assert(dto.getTransactionId != null, ErrorCode.INPUTERROR.getCode, "transactionId不能为空")
    assert(dto.getTransactionSequence != null, ErrorCode.INPUTERROR.getCode, "过程所属序列号不能为空")
    assert(dto.getStatus != null, ErrorCode.INPUTERROR.getCode, "过程当前状态不能为空")
    assert(dto.getExpectedStatus != null, ErrorCode.INPUTERROR.getCode, "过程目标状态不能为空")
    assert(dto.getServiceName != null, ErrorCode.INPUTERROR.getCode, "服务名称不能为空")
    assert(dto.getVersionName != null, ErrorCode.INPUTERROR.getCode, "服务版本不能为空")
    assert(dto.getMethodName != null, ErrorCode.INPUTERROR.getCode, "方法名称不能为空")
    assert(dto.getRollbackMethodName != null, ErrorCode.INPUTERROR.getCode, "回滚方法名称不能为空")
    assert(dto.getRequestJson != null, ErrorCode.INPUTERROR.getCode, "过程请求参数Json序列化不能为空")
    assert(dto.getResponseJson != null, ErrorCode.INPUTERROR.getCode, "过程响应参数Json序列化不能为空")
  }

  /**
    * 动作
    */
  override def action: TGlobalTransactionProcess = {

    val now: Date = new Date

    val process: GlobalTransactionProcess = new GlobalTransactionProcess {

      this.transactionId = dto.getTransactionId
      this.transactionSequence = dto.getTransactionSequence
      this.status = dto.getStatus.getValue
      this.expectedStatus = dto.getExpectedStatus.getValue
      this.serviceName = dto.getServiceName
      this.methodName = dto.getMethodName
      this.versionName = dto.getVersionName
      this.rollbackMethodName = dto.getRollbackMethodName
      this.requestJson = dto.getRequestJson
      this.responseJson = dto.getResponseJson
      this.redoTimes = 0
      this.updatedAt = new Timestamp(now.getTime)
      this.createdAt = new Timestamp(now.getTime)
      this.createdBy = dto.getCreatedBy
      this.updatedBy = dto.getCreatedBy
      this.nextRedoTime = new Timestamp(dto.getNextRedoTime.getTime)

    }

    dto.setId(TransactionSQL.insertTransactionProcess(process))

    LOGGER.info("创建事务过程({}),({}),({}),({}),({}),({}),({}),({}),({}),({}),({})", dto.getId.toString, dto.getTransactionId.toString,
      dto.getTransactionSequence.toString, dto.getStatus.getValue.toString, dto.getExpectedStatus.toString, dto.getServiceName, dto.getMethodName, dto.getVersionName,
      dto.getRollbackMethodName, dto.getRequestJson, dto.getResponseJson);

    dto
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
