package com.github.j5ik2o.reactive.memcached

import java.util.concurrent.Executors

import org.scalatest.{ BeforeAndAfterAll, Suite }

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutorService }

@SuppressWarnings(
  Array("org.wartremover.warts.Null", "org.wartremover.warts.Var", "org.wartremover.warts.MutableDataStructures")
)
trait MemcachedSpecSupport extends Suite with BeforeAndAfterAll {

  private var _memcachedTestServer: MemcachedTestServer = _

  def memcachedTestServer: MemcachedTestServer = _memcachedTestServer

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    implicit val ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())
    startMemcached()
  }

  def startMemcached()(implicit ec: ExecutionContext): Unit = {
    _memcachedTestServer = new MemcachedTestServer()
    _memcachedTestServer.start()
  }

  override protected def afterAll(): Unit = {
    stopMemached()
    super.afterAll()
  }

  def stopMemached(): Unit = {
    _memcachedTestServer.stop()
  }

}
