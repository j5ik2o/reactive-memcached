package com.github.j5ik2o.reactive.memcached

import org.scalatest.{ BeforeAndAfterAll, Suite }

import scala.concurrent.ExecutionContext

trait MemcachedSpecSupport extends Suite with BeforeAndAfterAll {

  private var _memcachedTestServer: MemcachedTestServer = _

  def memcachedTestServer: MemcachedTestServer = _memcachedTestServer

  def startMemcached()(implicit ec: ExecutionContext): Unit = {
    _memcachedTestServer = new MemcachedTestServer()
    _memcachedTestServer.start()
  }

  def stopMemached(): Unit = {
    _memcachedTestServer.stop()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    import scala.concurrent.ExecutionContext.Implicits.global
    startMemcached()
  }

  override protected def afterAll(): Unit = {
    stopMemached()
    super.afterAll()
  }

}
