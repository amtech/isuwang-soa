package com.isuwang.scala.dbc

import java.nio.ByteBuffer
import java.sql.Timestamp
import java.util.{Optional, Date}
import java.{lang, math, util}

import com.isuwang.org.apache.thrift.TBase
import com.isuwang.scala.dbc.helper.BeanConverterHelper
import com.isuwang.scala.dbc.utils.ThriftBeanConverter
import com.isuwang.scala.dbc.utils.ThriftBeanConverter.IBean
import wangzx.scala_commons.sql.Row.Cell

import scala.collection.mutable.ListBuffer
import scala.reflect.ClassTag

/**
  * Created by caiwb on 15-10-29.
  */
object Implicit {

  implicit class ListImplicit[T](list: List[T]) {
    def isEmpty(): Boolean = list == null || list.size == 0

    def isNotEmpty(): Boolean = !isEmpty()
  }

  implicit class ObjectImplicit[T](t: T) {

    def isBlank(): Boolean = isEmpty

    def isEmpty(): Boolean =
      if (t == null || t == None || t == Nil) true
      else t match {
        case n: Int => n == 0
        case n: Short => n == 0
        case n: Long => n == 0
        case s: String => s.isEmpty
        case map: java.util.Map[_, _] => map.isEmpty
        case opt: java.util.Optional[_] => !opt.isPresent
        case _ => false
      }


    def isNotEmpty(): Boolean = !isEmpty

    def in(values: List[T]): Boolean = values.exists(v => v == t)

    def notIn(values: List[T]): Boolean = !in(values)
  }

  implicit class TimestampImplicit(t: Timestamp) {
    def apply(date: Date): Unit = new Timestamp(new Date().getTime)
  }

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

  implicit class SqlBooleanImplicit(b: Boolean) {

    import wangzx.scala_commons.sql._

    def default(): SQLWithArgs = sql""

    // call by name
    def optional(left: => SQLWithArgs, right: => SQLWithArgs = null): SQLWithArgs =
      if (b) left
      else {
        val r = right
        if (r != null) r
        else default
      }

    //def optional (value: SQLWithArgs, option: SQLWithArgs = null): SQLWithArgs = if (b) value else if (option != null) option else default

    def optional(f: => SQLWithArgs): SQLWithArgs = if (b) f else default

  }

  implicit class LongImplicit(l: Long) {
    def toDate(): Date = new Date(l)
  }

  implicit class JavaLongImplicit(l: java.lang.Long) {
    def toDate(): Date = new Date(l)
  }

  implicit class ByteBufferImplicit(byteBuffer: ByteBuffer) {
    def toArray(): Array[Byte] = if (byteBuffer != null) java.util.Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit()) else null
  }

  implicit class DoubleEx(value: Double) {
    def toBigDecimal = BigDecimal(value)
    def toRoundBigDecimal = {
      BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP);
    }
  }

  implicit class JavaDoubleEx(value: java.lang.Double) {
    def toBigDecimal = BigDecimal(value)
    def toRoundBigDecimal = {
      BigDecimal(value).setScale(2, BigDecimal.RoundingMode.HALF_UP);
    }
  }

  implicit class BeanThriftEx[O <: AnyRef](value: O) {

    def toThrift[T <: AnyRef : ClassTag]: T = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
      BeanConverterHelper.copy(value, clazzT, false)
    }
  }

  implicit class BeanThriftEx2[O <: List[AnyRef]](value: O) {

    def toThrifts[T <: AnyRef : ClassTag](): List[T] = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

      val buffer: ListBuffer[T] = new ListBuffer[T]
      for (v: AnyRef <- value) {
        buffer.+=(BeanConverterHelper.copy(v, clazzT, false))
      }

      buffer.toList
    }
  }

  implicit class BeanDbCEntityEx[O <: AnyRef](value: O) {

    def toDbcEntity[T <: AnyRef : ClassTag]: T = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
      BeanConverterHelper.copy(value, clazzT, false)
    }

  }

  implicit class JavaBigDecimalEx(value: java.math.BigDecimal) {
    def toScalaBigDecimal = BigDecimal(value.doubleValue())
  }

  implicit class RowThriftEx[R <: wangzx.scala_commons.sql.Row](value: R) {

    def toThrift[T <: TBase[_, _] : ClassTag]: T = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

      val map: util.Map[String, IBean] = new util.HashMap[String, IBean]();

      value.cells.map { cell =>
        val rowBean = new RowBean(cell)

        map.put(cell.name, rowBean)
      }

      return ThriftBeanConverter.copy(map, clazzT)
    }

  }

  /**
    * 可用于计算列表中带条件的运算.
    * 例如查找 从 1 * 2 * .. * N 中不大于100的 N
    * @param seq
    * @tparam A
    */
  implicit class SeqX[A](seq: List[A]) {
    def foldLeftWhile[B](z: B)(op: (B, A) => (B, Boolean)): B = {
      var aggr = z
      var continue = true
      var remains = seq

      while (continue) {
        remains match {
          case h :: tail =>
            val (a, c) = op(aggr, h)
            aggr = a
            continue = c
            remains = tail
          case Nil =>
            continue = false
        }
      }

      aggr
    }
  }


  class RowBean(cell: Cell[_]) extends IBean {

    override def getDouble: lang.Double = cell.getDouble

    override def getBigDecimal: math.BigDecimal = cell.getBigDecimal

    override def getFloat: lang.Float = cell.getFloat

    override def getLong: lang.Long = cell.getLong

    override def getByte: lang.Byte = cell.getByte

    override def getBoolean: lang.Boolean = cell.getBoolean

    override def getShort: lang.Short = cell.getShort

    override def getObject: AnyRef = cell.getObject

    override def getInteger: Integer = cell.getInt

    override def getDate: Date = cell.getDate

    override def getString: String = cell.getString

    override def getCamel: String = {
      val cn = cell.name.toLowerCase

      var pos = 0;
      val len = cn.length
      val builder = StringBuilder.newBuilder

      while (pos < len) {
        val ch0: String = cn.substring(pos, pos + 1);
        val ch1: String = if (pos + 1 < len) cn.substring(pos + 1, pos + 2) else null

        if (ch0 != "_") {
          builder.append(ch0)

          pos = pos + 1;
        } else {
          if (ch1 != null && ch1 != "_") {
            builder.append(ch1.toUpperCase)

            pos = pos + 2
          } else {
            pos = pos + 1
          }
        }
      }

      return builder.toString
    }
  }

  implicit class BeanThriftEx3[O <: AnyRef](value: O) {
    def thriftToThrift[T <: AnyRef : ClassTag]: T = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]
      com.isuwang.scala.dbc.utils.ThriftBeanConverter.copy(value, clazzT)
    }
  }


  implicit class BeanThriftEx4[O <: List[AnyRef]](value: O) {

    def thriftToThrifts[T <: AnyRef : ClassTag](): List[T] = {
      val clazzT: Class[T] = implicitly[ClassTag[T]].runtimeClass.asInstanceOf[Class[T]]

      val buffer: ListBuffer[T] = new ListBuffer[T]
      for (v: AnyRef <- value) {
        buffer.+=(com.isuwang.scala.dbc.utils.ThriftBeanConverter.copy(v, clazzT))
      }

      buffer.toList
    }
  }

  implicit class BeanOptional[O <: AnyRef](value: O) {
    def toOptional: Optional[O] = {
      Optional.of(value)
    }
  }

}
