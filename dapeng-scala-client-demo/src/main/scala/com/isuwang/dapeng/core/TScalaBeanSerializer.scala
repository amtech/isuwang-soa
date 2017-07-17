package com.isuwang.dapeng.core

import com.isuwang.org.apache.thrift.TException
import com.isuwang.org.apache.thrift.protocol.TProtocol

/**
  * Created by ever on 2017/7/17.
  */
trait TScalaBeanSerializer[T] {
  @throws[TException]
  def read(iproto: TProtocol): T

  @throws[TException]
  def write(bean: T, oproto: TProtocol)

  @throws[TException]
  def validate(bean: T)

  def toString(bean: T): String
}
