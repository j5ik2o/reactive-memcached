package com.github.j5ik2o.reactive.memcached.pool

import akka.actor.{ Actor, ActorLogging, ActorSystem, Props }
import akka.pattern.pipe
import akka.stream.Supervision
import akka.util.Timeout
import com.github.j5ik2o.reactive.memcached.command.{ CommandRequest, CommandResponse }
import com.github.j5ik2o.reactive.memcached.pool.MemcachedConnectionActor.{ BorrowConnection, ConnectionGotten }
import com.github.j5ik2o.reactive.memcached.{ MemcachedConnection, PeerConfig }
import monix.execution.Scheduler

import scala.concurrent.duration.FiniteDuration

object MemcachedConnectionActor {

  def props(peerConfig: PeerConfig,
            newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
            supervisionDecider: Option[Supervision.Decider],
            passingTimeout: FiniteDuration)(
      implicit scheduler: Scheduler
  ): Props =
    Props(new MemcachedConnectionActor(peerConfig, newConnection, supervisionDecider, passingTimeout))

  case object BorrowConnection
  final case class ConnectionGotten(memcachedConnection: MemcachedConnection)

}

class MemcachedConnectionActor(peerConfig: PeerConfig,
                               newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                               supervisionDecider: Option[Supervision.Decider],
                               passingTimeout: FiniteDuration)(
    implicit scheduler: Scheduler
) extends Actor
    with ActorLogging {

  private implicit val as: ActorSystem        = context.system
  private val connection: MemcachedConnection = newConnection(peerConfig, supervisionDecider)
  implicit val to: Timeout                    = passingTimeout

  override def receive: Receive = {
    case cmdReq: CommandRequest =>
      connection.send(cmdReq).runToFuture.mapTo[CommandResponse].pipeTo(sender())
    case BorrowConnection =>
      log.debug("msg = BorrowConnection")
      sender() ! ConnectionGotten(connection)
      log.debug(s"reply = ConnectionGotten($connection)")
  }

}
