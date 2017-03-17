package com.isuwang.dapeng.transaction.support

import java.lang.reflect.Method
import java.sql.{Connection, ResultSet}
import javax.sql.DataSource

import com.isuwang.org.apache.thrift.TEnum
import org.apache.commons.lang.math.NumberUtils
import org.springframework.jdbc.datasource.{ConnectionHolder, DataSourceUtils}
import org.springframework.transaction.NoTransactionException
import org.springframework.transaction.support.TransactionSynchronizationManager
import wangzx.scala_commons.sql.BeanMapping._
import wangzx.scala_commons.sql.{SQLWithArgs, _}

import scala.reflect.ClassTag

/**
  * Created by caiwb on 15-10-29.
  */
trait DB {

  def withDataSource(): DataSource

  object ScalaBigDecimalJdbcValueMapper extends JdbcValueMapper[scala.math.BigDecimal] {
    override def getJdbcValue(bean: scala.math.BigDecimal): AnyRef = new java.math.BigDecimal(bean.asInstanceOf[scala.math.BigDecimal].toString)

    override def getBeanValue(field: AnyRef, `type`: Class[scala.math.BigDecimal]): scala.math.BigDecimal = scala.math.BigDecimal(field.toString)
  }

  object EnumJdbcValueMapper extends JdbcValueMapper[java.lang.Enum[_]] {
    override def getJdbcValue(bean: java.lang.Enum[_]): AnyRef = getJdbcValueImpl(bean)

    override def getBeanValue(field: AnyRef, `type`: Class[java.lang.Enum[_]]): java.lang.Enum[_] = getBeanValueImpl(field, `type`)

    private def getJdbcValueImpl(fieldValue: java.lang.Enum[_]): AnyRef = {
      if (classOf[TEnum].isAssignableFrom(fieldValue.getClass)) new Integer(fieldValue.asInstanceOf[TEnum].getValue)
      else fieldValue.name
    }

    private def getBeanValueImpl(dbValue: Any, fieldType: Class[_]): java.lang.Enum[_] = {

      val enumConstants = fieldType.getEnumConstants
      for (enumConstant <- enumConstants) {
        if (dbValue.getClass == ClassOfString) {
          if (enumConstant.asInstanceOf[java.lang.Enum[_]].name == dbValue.toString) return enumConstant.asInstanceOf[java.lang.Enum[_]]
        } else if (NumberUtils.isNumber(dbValue.toString)) {
          if (enumConstant.isInstanceOf[TEnum]) {
            if (enumConstant.asInstanceOf[TEnum].getValue == NumberUtils.toInt(dbValue.toString)) return enumConstant.asInstanceOf[java.lang.Enum[_]]
          } else {
            if (enumConstant.asInstanceOf[java.lang.Enum[_]].ordinal == NumberUtils.toInt(dbValue.toString)) return enumConstant.asInstanceOf[java.lang.Enum[_]]
          }
        }
      }

      null
    }
  }

  object DbcJdbcValueMapperFactory extends JdbcValueMapperFactory {
    def getJdbcValueMapper[T](`type`: Class[T]) =
      if (classOf[scala.math.BigDecimal].isAssignableFrom(`type`))
        ScalaBigDecimalJdbcValueMapper.asInstanceOf[JdbcValueMapper[T]]
      else if (classOf[java.lang.Enum[_]].isAssignableFrom(`type`))
        EnumJdbcValueMapper.asInstanceOf[JdbcValueMapper[T]]
      else null
  }

  private def withConnection[T](f: RichConnection => T): T = {
    val conn = getConnection

    f(new RichConnection(conn)(DbcJdbcValueMapperFactory))

    /*
    try {
      f(new RichConnection(conn)(DbcJdbcValueMapperFactory))
    } finally {
      releaseConnection(conn)
    }
    */
  }

  def buildSqlIn[T](list: List[T]): SQLWithArgs = {
    val sql =
      if (list != null && list.size > 0) {
        "('" + (list.map(_.toString).toSeq.mkString("','")) + "')"
      } else {
        "()"
      }
    SQLWithArgs(sql, Nil)
  }

  def getConnection(): Connection = {
    val dataSource = withDataSource

    val conHolder: ConnectionHolder = TransactionSynchronizationManager.getResource(dataSource).asInstanceOf[ConnectionHolder]
    //if (conHolder != null && (conHolder.hasConnection || conHolder.isSynchronizedWithTransaction)) {

    if (conHolder != null) {
      val hasConnectionMethod: Method = conHolder.getClass.getDeclaredMethod("hasConnection")
      hasConnectionMethod.setAccessible(true)
      val hasConnection: Boolean = hasConnectionMethod.invoke(conHolder).asInstanceOf[Boolean]

      if (hasConnection == true || conHolder.isSynchronizedWithTransaction)
        return DataSourceUtils.getConnection(withDataSource)
    }

    throw new NoTransactionException("No transaction aspect-managed TransactionStatus in scope")
  }

  def releaseConnection(conn: Connection): Unit = {
    DataSourceUtils.releaseConnection(conn, withDataSource)
  }

  implicit class EsqlStringInterpolation(sc: StringContext) {

    def esql(args: Any*): Int =
      esqlWithGenerateKey(args: _*)(null)

    def esqlWithGenerateKey(args: Any*)(processGenerateKeys: ResultSet => Unit = null): Int = {
      val stmt: SQLWithArgs = SQLWithArgs(sc.parts.mkString("?"), args)

      checkSql(stmt)

      withConnection(_.executeUpdateWithGenerateKey(stmt)(processGenerateKeys))
    }

  }

  def eachRow[T <: AnyRef](sql: SQLWithArgs)(f: T => Unit)(implicit ct: ClassTag[T]) =
    withConnection(_.eachRow(sql)(f)(ct))

  def rows[T <: AnyRef](sql: SQLWithArgs)(implicit ct: ClassTag[T]): List[T] = withConnection(_.rows(sql)(ct))

  def row[T <: AnyRef](sql: SQLWithArgs)(implicit ct: ClassTag[T]): Option[T] = withConnection(_.row(sql)(ct))

  def insert(bean: AnyRef, excludeColumns: List[String] = Nil) = {
    val op = new BeanOperation(bean)(DbcJdbcValueMapperFactory).ignoreNullField(true).excludeColumn(excludeColumns: _*)
    withConnection(op.insert(_))
  }

  def update(bean: AnyRef, ignoreNullField: Boolean = true, ignoreEmptyField: Boolean = true, includeColumns: List[String] = Nil) = {
    val op = new BeanOperation(bean)(DbcJdbcValueMapperFactory).ignoreNullField(ignoreNullField).ignoreEmptyField(ignoreEmptyField)
    if (includeColumns != Nil) op.excludeAllColumns().includeColumn(includeColumns: _*)
    withConnection(op.update(_))
  }

  def delete(bean: AnyRef) = {
    val op = new BeanOperation(bean)(DbcJdbcValueMapperFactory)
    withConnection(op.delete(_))
  }

  def esql(stmt: SQLWithArgs): Int =
    esqlWithGenerateKey(stmt)(null)

  def esqlWithGenerateKey(stmt: SQLWithArgs)(processGenerateKeys: ResultSet => Unit = null): Int = {
    checkSql(stmt)

    withConnection(_.executeUpdateWithGenerateKey(stmt)(processGenerateKeys))
  }

  def queryInt(stmt: SQLWithArgs): Int = withConnection(_.queryInt(stmt))

  def checkSql(stmt: SQLWithArgs): Unit = {

    val sql = stmt.sql.toLowerCase.trim
    if (sql.startsWith("update") || sql.startsWith("delete"))
      assert(sql.contains("where"), "UPDATE and DELETE sql must have WHERE condition")
  }

  def insertSql(stmt: SQLWithArgs): Int = {
    var index = 0
    esqlWithGenerateKey(stmt) { rs: ResultSet =>
      if (rs.next()) index = rs.getInt(1)
    }
    index
  }
}