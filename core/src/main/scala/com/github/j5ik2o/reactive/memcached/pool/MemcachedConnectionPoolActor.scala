package com.github.j5ik2o.reactive.memcached.pool
import java.util.concurrent.atomic.AtomicLong

import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.routing.Pool
import cats.data.NonEmptyList

object MemcachedConnectionPoolActor {

  def props(pool: Pool, connectionProps: NonEmptyList[Props]): Props =
    Props(new MemcachedConnectionPoolActor(pool, connectionProps))

}

class MemcachedConnectionPoolActor(pool: Pool, connectionProps: NonEmptyList[Props]) extends Actor with ActorLogging {

  private val index = new AtomicLong(0L)

  private val routers: NonEmptyList[ActorRef] = connectionProps.map(p => context.actorOf(pool.props(p)))

  override def receive: Receive = {
    case msg =>
      log.debug("msg = {}", msg)
      routers.toList(index.getAndIncrement().toInt % routers.size) forward msg
  }

}
