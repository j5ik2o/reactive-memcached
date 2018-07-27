package com.github.j5ik2o.reactive.memcached

import java.io._
import java.net.InetSocketAddress

import com.github.j5ik2o.reactive.memcached.util.{ JarUtil, OSType }
import org.slf4j.{ Logger, LoggerFactory }

import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

class MemcachedTestServer(portOpt: Option[Int] = None,
                          masterPortOpt: Option[Int] = None,
                          forbiddenPorts: Seq[Int] = 6300.until(7300)) {
  lazy val logger: Logger = LoggerFactory.getLogger(getClass)
  @volatile
  private[this] var process: Option[Process] = None
  @volatile
  private var _address: Option[InetSocketAddress] = None

  def getPort: Int = portOpt.getOrElse(_address.get.getPort)

  def address: Option[InetSocketAddress] = _address

  val path: String = JarUtil
    .extractExecutableFromJar(if (OSType.ofAuto == OSType.macOS) {
      "memcached-1.5.9.macOS"
    } else {
      "memcached-1.5.9.Linux"
    })
    .getPath

  private[this] def assertMemcachedBinaryPresent()(implicit ec: ExecutionContext): Unit = {
    val p = new ProcessBuilder(path, "--help").start()
    printlnStreamFuture(new BufferedReader(new InputStreamReader(p.getInputStream)))
    printlnStreamFuture(new BufferedReader(new InputStreamReader(p.getErrorStream)))
    p.waitFor()
    val exitValue = p.exitValue()
    require(exitValue == 0 || exitValue == 1, "memcached binary must be present.")
  }

  private[this] def findAddress(): InetSocketAddress = {
    var tries = 100
    while (_address.isEmpty && tries >= 0) {
      _address = Some(RandomPortSupport.temporaryServerAddress())
      if (forbiddenPorts.contains(_address.get.getPort)) {
        _address = None
        tries -= 1
        logger.info("try to get port...")
        Thread.sleep(5)
      }
    }
    val result = _address.getOrElse {
      sys.error("Couldn't get an address for the external memcached instance")
    }
    logger.info(s"findAddress: ${_address}")
    result
  }

  private def printlnStreamFuture(br: BufferedReader)(implicit ec: ExecutionContext): Future[Unit] = {
    val result = Future {
      br.readLine()
    }.flatMap { result =>
        if (result != null) {
          logger.debug(result)
          printlnStreamFuture(br)
        } else
          Future.successful(())
      }
      .recoverWith {
        case ex =>
          Future.successful(())
      }
    result.onComplete {
      case Success(_) =>
        br.close()
      case Failure(ex) =>
        logger.error("Occurred error", ex)
        br.close()
    }
    result
  }

  def start()(implicit ec: ExecutionContext): Unit = {
    assertMemcachedBinaryPresent()
    findAddress()
    logger.info("memcached test server will be started")
    val port             = getPort
    val cmd: Seq[String] = Seq(path, s"--port=$port")
    val builder          = new ProcessBuilder(cmd.asJava)
    val _process         = builder.start()
    printlnStreamFuture(new BufferedReader(new InputStreamReader(_process.getInputStream)))
    printlnStreamFuture(new BufferedReader(new InputStreamReader(_process.getErrorStream)))
    process = Some(_process)
    Thread.sleep(200)
    logger.info("memcached test server has started")
  }

  def stop(): Unit = {
    process.foreach { p =>
      logger.info("memcached test server will be stopped")
      p.destroy()
      p.waitFor()
      logger.info("memcached test server has stopped")
    }
  }

  def restart()(implicit ec: ExecutionContext): Unit = {
    stop()
    start()
  }
}
