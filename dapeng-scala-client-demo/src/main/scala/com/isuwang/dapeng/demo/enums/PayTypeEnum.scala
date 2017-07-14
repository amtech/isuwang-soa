package com.isuwang.dapeng.demo.enums

/**
  * 付款方式
  * Created by ever on 2017/7/14.
  */
object PayTypeEnum extends Enumeration{
  type PayTypeEnum = Value

  val WeChat = Value(1, "微信")
  val AliPay = Value(2, "支付宝")
  val Bank = Value(3, "银行转账")
}
