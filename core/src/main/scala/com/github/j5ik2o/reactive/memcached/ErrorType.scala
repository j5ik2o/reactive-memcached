package com.github.j5ik2o.reactive.memcached

import enumeratum._

import scala.collection.immutable

sealed trait ErrorType extends EnumEntry

object ErrorType extends Enum[ErrorType] {
  override def values: immutable.IndexedSeq[ErrorType] = findValues

  case object OtherType  extends ErrorType
  case object ClientType extends ErrorType
  case object ServerType extends ErrorType
}
