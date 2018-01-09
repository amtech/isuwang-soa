package com.isuwang.soa.service.test

import java.util.Optional
import java.util.concurrent.{CompletableFuture, TimeUnit}

import com.isuwang.soa.order.{OrderServiceAsyncClient, OrderServiceClient}
import com.isuwang.soa.order.domain.Order
import com.isuwang.soa.order.scala.domain.Order
import com.isuwang.soa.price.PriceServiceClient
import com.isuwang.soa.price.domain.Price
import com.isuwang.soa.price.{PriceServiceAsyncClient, domain}
import com.isuwang.soa.settle.{SettleServiceAsyncClient, SettleServiceClient}
import com.isuwang.soa.settle.domain.Settle
import com.isuwang.soa.settle.scala.domain.Settle
import com.isuwang.soa.user.{UserServiceAsyncClient, UserServiceClient, scala}
import com.isuwang.soa.user.domain.User
import _root_.scala.concurrent.ExecutionContext.Implicits.global
import _root_.scala.util.Success
import _root_.scala.util.Failure

object ServiceInvokeTest {

  /**
    *  priceServiceClient (java) -> PriceServiceServer (java Async)
    *  priceServiceClient (scala) -> PriceServiceServer (java Async)
    *  AsyncpriceServiceClient (java) -> PriceServiceServer (java Async)
    *  AsyncpriceServiceClient (scala) -> PriceServiceServer (java Async)
    *
    *  SettleServiceClient (java) -> SettleServiceServer (scala Async)
    *  SettleServiceClient (scala) -> SettleServiceServer (scala Async)
    *  AsyncSettleServiceClient (java) -> SettleServiceServer (scala Async)
    *  AsyncSettleServiceClient (scala) -> SettleServiceServer (scala Async)
    *
    *  UserServiceClient (java) -> UserServiceServer (java Sync)
    *  UserServiceClient (scala) -> UserServiceServer (java Sync)
    *  AsyncUserServiceClient (scala) -> UserServiceServer (java sync)
    *  AsyncUserServiceClient (java) -> UserServiceServer (java sync)
    *
    *  OrderServiceClient (java) -> OrderServiceServer (scala Sync)
    *  OrderServiceClient (scala) -> OrderServiceServer (scala Sync)
    *  AsyncOrderServiceClient (java) -> OrderServiceServer (scala sync)
    *  AsyncOrderServiceClient (scala) -> OrderServiceServer (scala sync)
    *
    * @param args
    */
  def main(args: Array[String]): Unit = {

    new PriceServiceClient().insertPrice(new Price().orderId(1).price(1000.0))
    new com.isuwang.soa.price.scala.PriceServiceClient().insertPrice(com.isuwang.soa.price.scala.domain.Price(2,2000.0))
    val javaPrices = new PriceServiceAsyncClient().getPrices(5000).get(3000, TimeUnit.MILLISECONDS)
    assert(javaPrices.size() == 2, " javaPrices size not match")
    new com.isuwang.soa.price.scala.PriceServiceAsyncClient().getPrices(5000).onComplete({
      case Success(scalaPrices) => assert(scalaPrices.size == 2, " scalaPrices size not match..")
      case Failure(e) => throw e
    })

    new SettleServiceClient().createSettle(new com.isuwang.soa.settle.domain.Settle()
      .id(1).orderId(1).cash_credit(1000).cash_debit(100).remark(Optional.of("settle remark")))
    new com.isuwang.soa.settle.scala.SettleServiceClient().createSettle(com.isuwang.soa.settle.scala.domain.Settle(2,2,2000,2000,Option.apply("scala settle remark")))
    val javaSettle = new SettleServiceAsyncClient().getSettleById(1,5000).get(3000, TimeUnit.MILLISECONDS)
    assert(javaSettle.orderId == 1, " java settle not match")
    new com.isuwang.soa.settle.scala.SettleServiceAsyncClient().getSettleById(2,5000).onComplete({
      case Success(scalaSettle) => assert(scalaSettle.orderId == 2, " scalaSettle not match..")
      case Failure(e) => throw e
    })


    new UserServiceClient().createUser(new com.isuwang.soa.user.domain.User().id(1).name("userName1").password("123456").phoneNumber("1234567890"))

    new com.isuwang.soa.user.scala.UserServiceClient().createUser(com.isuwang.soa.user.scala.domain.User(2,"userName2","123","123456"))
    val javaUser = new UserServiceAsyncClient().getUserById(1,5000).get(3000, TimeUnit.MILLISECONDS)
    assert(javaUser.name.equals("userName1"), " javaUser not match")
    new com.isuwang.soa.user.scala.UserServiceAsyncClient().getUserById(2,5000).onComplete({
      case Success(scalaUser) => assert(scalaUser.name.equals("userName2"), " scalaUser not match..")
      case Failure(e) => throw e
    })


    new OrderServiceClient().createOrder(new com.isuwang.soa.order.domain.Order().id(1).order_no("1").status(1).amount(1000))
    new com.isuwang.soa.order.scala.OrderServiceClient().createOrder(com.isuwang.soa.order.scala.domain.Order(2,"2",2,2000))

    val javaOrder = new OrderServiceAsyncClient().getOrderById(1,5000).get(3000, TimeUnit.MILLISECONDS)
    assert(javaOrder.order_no.equals("1"), " javaOrder not match")
    new com.isuwang.soa.order.scala.OrderServiceAsyncClient().getOrderById(2,5000).onComplete({
      case Success(scalaOrder) => assert(scalaOrder.order_no.equals("2"), " scalaUser not match..")
      case Failure(e) => throw e
    })



  }
}
