package com.isuwang.dapeng.demo

import com.isuwang.dapeng.demo.domain.{FindOrderRequest, Order}

/**
  * 订单服务
  * Created by ever on 2017/7/14.
  */
trait OrderService {
  /**
    * 查找订单
    * @param request
    * @return
    */
  def findOrder(request: FindOrderRequest): List[Order]
}
