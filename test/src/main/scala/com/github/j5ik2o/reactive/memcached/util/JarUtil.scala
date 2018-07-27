package com.github.j5ik2o.reactive.memcached.util
import java.io.File

import com.google.common.io.{ Files, Resources }
import org.apache.commons.io.FileUtils

object JarUtil {

  def extractExecutableFromJar(executable: String): File = {
    val tmpDir = Files.createTempDir
    tmpDir.deleteOnExit()
    val command = new File(tmpDir, executable)
    FileUtils.copyURLToFile(Resources.getResource(executable), command)
    command.deleteOnExit()
    command.setExecutable(true)
    command
  }

}
