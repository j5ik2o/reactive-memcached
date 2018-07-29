package com.github.j5ik2o.reactive.memcached

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import monix.execution.Scheduler
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

abstract class AbstractActorSpec(_system: ActorSystem)
    extends TestKit(_system)
    with FreeSpecLike
    with Matchers
    with BeforeAndAfterAll
    with TimeFactorSupport
    with ScalaFutures
    with PropertyChecks
    with MemcachedSpecSupport {

  implicit val scheduler = Scheduler(system.dispatcher)

  val logger = LoggerFactory.getLogger(getClass)

  def waitFor(): Unit = Thread.sleep((100 * timeFactor).toInt)

  implicit override val patienceConfig: PatienceConfig =
    PatienceConfig(timeout = 30 * timeFactor seconds, interval = 500 * timeFactor milliseconds)

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

}
