package com.github.j5ik2o.reactive.memcached.util

import java.io.{ BufferedReader, InputStreamReader }

import enumeratum._

import scala.collection.immutable

sealed trait ArchitectureType extends EnumEntry

@SuppressWarnings(
  Array("org.wartremover.warts.Var",
        "org.wartremover.warts.Null",
        "org.wartremover.warts.Serializable",
        "org.wartremover.warts.Equals")
)
object ArchitectureType extends Enum[ArchitectureType] {
  override def values: immutable.IndexedSeq[ArchitectureType] = findValues

  case object x86 extends ArchitectureType

  case object x64 extends ArchitectureType

  private def getWindowsArchitecture(): ArchitectureType = {
    val arch      = System.getenv("PROCESSOR_ARCHITECTURE")
    val wow64Arch = System.getenv("PROCESSOR_ARCHITEW6432")
    if (!arch.endsWith("64") && (wow64Arch == null || !wow64Arch.endsWith("64"))) x86
    else x64
  }

  private def getUnixArchitecture(): ArchitectureType = {
    var input: BufferedReader = null
    try {
      val proc = Runtime.getRuntime.exec("uname -m")
      input = new BufferedReader(new InputStreamReader(proc.getInputStream))
      val itr =
        Iterator.continually(input.readLine).takeWhile(line => line != null && line.length > 0 && line.contains("64"))
      if (itr.nonEmpty) x64 else x86
    } finally {
      if (input != null) input.close()
    }
  }

  @SuppressWarnings(
    Array("org.wartremover.warts.Var",
          "org.wartremover.warts.Null",
          "org.wartremover.Serializable",
          "org.wartremover.Equals")
  )
  private def getMacOSXArchitecture(): ArchitectureType = {
    var input: BufferedReader = null
    try {
      val proc = Runtime.getRuntime.exec("sysctl hw")
      input = new BufferedReader(new InputStreamReader(proc.getInputStream))

      val itr = Iterator.continually(input.readLine()).takeWhile { line =>
        line != null && (line.length <= 0 || !line.contains("cpu64bit_capable") || !line.trim.endsWith("1"))
      }
      if (itr.isEmpty) x86
      else
        x64
    } finally {
      if (input != null) input.close()
    }

  }

  def ofAuto: ArchitectureType = {
    OSType.ofAuto match {
      case OSType.Windows => getWindowsArchitecture()
      case OSType.Linux   => getUnixArchitecture()
      case OSType.macOS   => getMacOSXArchitecture()
      case OSType.Other   => throw new Exception
    }
  }
}
