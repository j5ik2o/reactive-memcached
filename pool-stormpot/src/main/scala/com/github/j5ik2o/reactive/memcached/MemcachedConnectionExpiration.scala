package com.github.j5ik2o.reactive.memcached
import akka.actor.ActorSystem
import monix.execution.Scheduler
import stormpot.{ Expiration, SlotInfo }

import scala.concurrent.Await
import scala.concurrent.duration.Duration

final case class MemcachedConnectionExpiration(validationTimeout: Duration)(implicit system: ActorSystem,
                                                                            scheduler: Scheduler)
    extends Expiration[MemcachedConnectionPoolable] {

  private val client = MemcachedClient()

  override def hasExpired(slotInfo: SlotInfo[_ <: MemcachedConnectionPoolable]): Boolean = {
    !Await.result(client.version().map(_.nonEmpty).run(slotInfo.getPoolable.memcachedConnection).runToFuture,
                  validationTimeout)
  }

}
