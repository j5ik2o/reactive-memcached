package com.github.j5ik2o.reactive.memcached

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import cats.data.ReaderT
import com.github.j5ik2o.reactive.memcached.command.{ CommandRequest, CommandResponse }
import monix.eval.Task
import monix.execution.Scheduler

object MemcachedConnectionPoolFlow {

  def apply(memcachedConnectionPool: MemcachedConnectionPool[Task], parallelism: Int = 1)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): Flow[CommandRequest, CommandResponse, NotUsed] =
    new MemcachedConnectionPoolFlow(memcachedConnectionPool, parallelism).toFlow

}

class MemcachedConnectionPoolFlow(memcachedConnectionPool: MemcachedConnectionPool[Task], parallelism: Int)(
    implicit system: ActorSystem
) {
  private def toFlow(implicit scheduler: Scheduler): Flow[CommandRequest, CommandResponse, NotUsed] =
    Flow[CommandRequest].mapAsync(parallelism) { cmd =>
      memcachedConnectionPool.withConnectionM(ReaderT(_.send(cmd))).runToFuture
    }
}
