package com.github.j5ik2o.reactive.memcached

import org.scalatest.{ BeforeAndAfterAll, Suite }

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

trait MemcachedSpecSupport extends RandomPortSupport with Suite with BeforeAndAfterAll {

  var testServer: MemcachedTestServer = _

  def startMemcached()(implicit ec: ExecutionContext): Unit = {
    testServer = new MemcachedTestServer()
    testServer.start()
  }

  def stopMemached(): Unit = {
    testServer.stop()
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    startMemcached()
  }

  override protected def afterAll(): Unit = {
    stopMemached()
    super.afterAll()
  }

}
