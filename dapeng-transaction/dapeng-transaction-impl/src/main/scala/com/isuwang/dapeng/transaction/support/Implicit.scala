package com.isuwang.dapeng.transaction.support

import scala.reflect.ClassTag

/**
  * Created by caiwb on 15-10-29.
  */
object Implicit {

  implicit class StringImplicit(value: String) {

    def isBlank(): Boolean = value == null || value.isEmpty

    def isEmpty(): Boolean = value == null || value.isEmpty

    def isNotEmpty(): Boolean = !isEmpty

    private def trueIndexOf(index: Int): Int = {
      val length = value.length
      if (index < 0)
        index + length
      else if (index > length)
        index - length
      else
        index
    }

    // 指定位置大写
    def toUpperCase(index: Int): String = {
      val trueIndex = trueIndexOf(index)
      val char = value.charAt(trueIndex)
      if (!char.isUpper) {
        value.updated(trueIndex, char.toString.toUpperCase).mkString
      } else
        value
    }

    // 指定位置小写
    def toLowerCase(index: Int): String = {
      val trueIndex = trueIndexOf(index)
      val char = value.charAt(trueIndex)
      if (!char.isLower) {
        value.updated(trueIndex, char.toString.toLowerCase).mkString
      } else
        value
    }
  }


  implicit class BeanThriftEx[O <: AnyRef](value: O) {

    def toThrift[T <: AnyRef : ClassTag]: T = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
      BeanConverterHelper.copy(value, clazzT, false)
    }
  }


  implicit class ListConverterEx[F <: List[AnyRef]](from: F) {

    def toThrifts[T <: AnyRef : Manifest]: List[T] = {
      val toClass: Class[T] = implicitly[Manifest[T]].runtimeClass.asInstanceOf[Class[T]]
      BeanConverterHelper.copys(from.asInstanceOf[List[AnyRef]], toClass)
    }
  }

}
