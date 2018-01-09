package com.isuwang.soa.scala.service

import com.isuwang.soa.order.scala.domain.Order
import com.isuwang.soa.order.scala.service.OrderService

import scala.collection.mutable

class OrderServiecSyncImpl extends OrderService{

  val orders = mutable.HashMap[Integer,Order]()

  /**
    *
    **/
  override def createOrder(order: Order): Unit = {
    println("=============== scala createOrder. =========")
    orders.put(order.id,order)
  }

  /**
    *
    **/
  override def getOrderById(orderId: Int): Order = {
    println("=============== scala getOrderById. =========")
    orders.get(orderId) match {
      case Some(o) => o
      case None => throw new Exception(s"Failed to find order..orderId: ${orderId}")
    }
  }
}
