package com.github.j5ik2o.reactive.memcached

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cats.data.NonEmptyList
import monix.eval.Task
import org.scalatest.{ BeforeAndAfterAll, FreeSpecLike, Matchers }
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.prop.PropertyChecks
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

  val logger = LoggerFactory.getLogger(getClass)

  def waitFor(): Unit = Thread.sleep((100 * timeFactor).toInt)

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(3 * timeFactor seconds)

  implicit val materializer: ActorMaterializer = ActorMaterializer()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    shutdown()
    super.afterAll()
  }

}
