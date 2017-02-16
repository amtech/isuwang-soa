package com.isuwang.dapeng.transaction.support

import org.slf4j.{Logger, LoggerFactory}

/**
  * Design by contract Handler
 *
  * @author craneding
  * @date 15/12/4
  */
abstract class Action[Output] {
  def execute: Output = {
    try {
      inputCheck

      preCheck

      val output: Output = action

      postCheck

      output
    } catch {
      case e: Throwable =>
        val logger: Logger = LoggerFactory.getLogger("com.isuwang.scala.dbc")

        logger.error(e.getMessage, e)

        throw e
    } finally {
    }
  }

  /**
    * 输入检查：查询、新增、更新、删除等输入条件
    */
  def inputCheck

  /**
    * 前置条件检查：动作、状态等业务逻辑
    */
  def preCheck

  /**
    * 动作
    */
  def action: Output

  /**
    * 后置条件检查
    */
  def postCheck
}

