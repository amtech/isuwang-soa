package com.isuwang.dapeng.demo

import com.isuwang.dapeng.demo.domain.{FindOrderRequest, Order}
import com.isuwang.dapeng.demo.enums.PayTypeEnum

/**
  * 订单服务
  * Created by ever on 2017/7/14.
  */
class OrderServiceTest {
  /**
    * 查找订单
    * @param request
    * @return
    */
  def findOrderTest(request: FindOrderRequest): List[Order] = {
    val request = FindOrderRequest(payType = PayTypeEnum.AliPay, buyerId = 1)
    return List()
  }
}
