package com.github.j5ik2o.reactive.memcached

import enumeratum._

import scala.collection.immutable

sealed trait PoolType extends EnumEntry

object PoolType extends Enum[PoolType] {
  override def values: immutable.IndexedSeq[PoolType] = findValues
  case object Blaze extends PoolType
  case object Queue extends PoolType
}
