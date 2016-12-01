package com.isuwang.scala.dbc.helper

import java.lang.reflect.{ParameterizedType, Method}

import com.isuwang.org.apache.thrift.TEnum
import com.isuwang.scala.dbc.Implicit
import Implicit.StringImplicit
import wangzx.scala_commons.sql.SoftMap

/**
  * Created by caiwb on 15-8-8.
  */
object BeanMapping {

  val ClassOfByte = classOf[java.lang.Byte]
  val ClassOfChar = classOf[java.lang.Character]
  val ClassOfShort = classOf[java.lang.Short]
  val ClassOfInteger = classOf[java.lang.Integer]
  val ClassOfOption = classOf[scala.Option[Int]]
  val ClassOfJavaOption = classOf[java.util.Optional[Int]]
  val ClassOfLong = classOf[java.lang.Long]
  val ClassOfFloat = classOf[java.lang.Float]
  val ClassOfDouble = classOf[java.lang.Double]
  val ClassOfBoolean = classOf[java.lang.Boolean]
  val ClassOfString = classOf[java.lang.String]
  val ClassOfSQLDate = classOf[java.sql.Date]
  val ClassOfUtilDate = classOf[java.util.Date]
  val ClassOfSQLTime = classOf[java.sql.Time]
  val ClassOfSQLTimestamp = classOf[java.sql.Timestamp]
  val ClassOfBigDecimal = classOf[java.math.BigDecimal]
  val ClassOfScalaBigDecimal = classOf[scala.math.BigDecimal]
  val ClassOfByteArray = classOf[Array[Byte]]

  val ClassOfEnum = classOf[java.lang.Enum[_]]
  val ClassOfTEnum = classOf[TEnum]

  val ClassOfByteBuffer = classOf[java.nio.ByteBuffer]

  val G_BeanMappings = new SoftMap[Class[_], BeanMapping[_]]()

  /**
    * for a scala anonymous class, automate choose the parent
    */
  val annomous_regexp =
    """[$]*anon\$\d+""".r

  def real_class(clazz: Class[_]): Class[_] = clazz.getSimpleName match {
    case annomous_regexp() => real_class(clazz.getSuperclass)
    case _ => clazz
  }

  def isSupportedDataType(typ: Class[_]): Boolean = typ match {
    case java.lang.Boolean.TYPE | ClassOfBoolean => true
    case java.lang.Byte.TYPE | ClassOfByte => true
    case java.lang.Short.TYPE | ClassOfShort => true
    case java.lang.Integer.TYPE | ClassOfInteger => true
    case java.lang.Long.TYPE | ClassOfLong => true
    case java.lang.Float.TYPE | ClassOfFloat => true
    case java.lang.Double.TYPE | ClassOfDouble => true
    case ClassOfBigDecimal => true
    case ClassOfScalaBigDecimal => true
    case ClassOfSQLDate => true
    case ClassOfSQLTime => true
    case ClassOfSQLTimestamp | ClassOfUtilDate => true
    case ClassOfString => true
    case ClassOfByteArray => true
    case ClassOfEnum => true
    case ClassOfTEnum => true
    case ClassOfByteBuffer => true
    case ClassOfOption => true
    case ClassOfJavaOption => true
    case _ => false
  }

  def getBeanMapping[T](clazz: Class[T]): BeanMapping[T] = {
    synchronized {
      val cached: Option[BeanMapping[_]] = G_BeanMappings.get(clazz)
      cached match {
        case Some(result) =>
          result.asInstanceOf[BeanMapping[T]]

        case None =>
          val realClass = real_class(clazz)
          val mapping = new UnionBeanMapping(realClass)
          G_BeanMappings(clazz) = mapping
          return mapping.asInstanceOf[BeanMapping[T]]
      }
    }
  }
}

trait BeanMapping[E] {

  trait FieldMapping[F] {
    val fieldName: String
    val fieldType: Class[F]
    val optionalInnerType: Class[_]

    def get(bean: E): F

    def set(bean: E, value: F): Unit
  }

  val reflectClass: Class[E]
  val fields: List[FieldMapping[_]]

  def getFieldByName(name: String): Option[FieldMapping[_]]
}

class UnionBeanMapping[E](val reflectClass: Class[E]) extends BeanMapping[E] {

  val fields = getMappingFields
  val fieldsByName: Map[String, FieldMapping[_]] = fields.map { field =>
    (field.fieldName, field)
  }.toMap

  def newFieldMapping[T](name: String, getter: Method, setter: Method): FieldMapping[T] = new FieldMapping[T] {
    val fieldType = getter.getReturnType.asInstanceOf[Class[T]]
    val fieldName = name

    val _javaField: java.lang.reflect.Field = {
      // TODO inherits
      reflectClass.getDeclaredField(fieldName)
    }

    val optionalInnerType = fieldType match {
      case BeanMapping.ClassOfJavaOption =>
        if (_javaField.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments().apply(0).isInstanceOf[ParameterizedType])
          _javaField.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments().apply(0).asInstanceOf[ParameterizedType].getActualTypeArguments.apply(0).asInstanceOf[Class[_]]
        else
          _javaField.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments().apply(0).asInstanceOf[Class[_]]
      case _ =>
        null
    }

    def get(bean: E) = getter.invoke(bean).asInstanceOf[T]

    def set(bean: E, value: T) {
      setter.invoke(bean, value.asInstanceOf[AnyRef])
    }
  }

  def getFieldByName(name: String) = fieldsByName.get(name)

  /**
    * support 2 styles mapping:
    * 1. scala style. eg: name() for getter and name_=(arg) for setter
    * 2. JavaBean Style. eg: getName()/isName() setName()
    */
  def getMappingFields: List[FieldMapping[_]] = {

    val getters: Map[String, Method] = reflectClass.getMethods.filter { method =>
      method.getParameterTypes.length == 0 && (BeanMapping.isSupportedDataType(method.getReturnType) || BeanMapping.ClassOfEnum.isAssignableFrom(method.getReturnType))
    }.map { method => (method.getName, method) }.toMap

    val setters: Map[String, Method] = reflectClass.getMethods.filter { method =>
      method.getParameterTypes.length == 1 && (BeanMapping.isSupportedDataType(method.getParameterTypes.apply(0)) || BeanMapping.ClassOfEnum.isAssignableFrom(method.getParameterTypes.apply(0))) &&
        method.getReturnType == Void.TYPE || method.getReturnType == reflectClass
    }.map { method => (method.getName, method) }.toMap

    val mappings: Iterable[FieldMapping[_]] = getters.keys.flatMap { name =>

      // style: name(), name_=(arg)
      val scala = for (getter <- getters.get(name);
                       setter <- setters.get(name + "_$eq");
                       if (getter.getReturnType == setter.getParameterTypes.apply(0))
      ) yield newFieldMapping(name, getter, setter)

      // style: isName() setName(arg)
      val is = for (getter <- getters.get(name) if name.startsWith("is") && getter.getReturnType == classOf[Boolean];
                    setter <- setters.get("set" + name.substring(2));
                    if (getter.getReturnType == setter.getParameterTypes.apply(0))
      ) yield newFieldMapping(name.substring(2).toLowerCase(0), getter, setter)

      // style: getName() setName(arg)
      val get = for (getter <- getters.get(name) if name.startsWith("get");
                     setter <- setters.get("set" + name.substring(3));
                     if (getter.getReturnType == setter.getParameterTypes.apply(0))
      ) yield newFieldMapping(name.substring(3).toLowerCase(0), getter, setter)

      scala.orElse(is).orElse(get)
    }

    mappings.toList

  }

}