package com.github.j5ik2o.reactive.memcached.util

import enumeratum._

import scala.collection.immutable

sealed trait OSType extends EnumEntry

object OSType extends Enum[OSType] {
  override def values: immutable.IndexedSeq[OSType] = findValues

  case object Windows extends OSType

  case object Linux extends OSType

  case object macOS extends OSType

  case object Other extends OSType

  def ofAuto: OSType = {
    val osName = System.getProperty("os.name").toLowerCase
    if (osName.contains("win")) Windows
    else if ("Mac OS X".equalsIgnoreCase(osName)) macOS
    else if ("Linux".equalsIgnoreCase(osName)) Linux
    else Other
  }

}
