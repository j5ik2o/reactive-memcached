package com.github.j5ik2o.reactive.memcached

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Supervision
import cats.data.NonEmptyList
import io.github.andrebeat.pool._
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration._

object ScalaPool {

  def ofSingle(connectionPoolConfig: ScalaPoolConfig,
               peerConfig: PeerConfig,
               newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
               supervisionDecider: Option[Supervision.Decider] = None)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): ScalaPool = new ScalaPool(connectionPoolConfig, NonEmptyList.of(peerConfig), newConnection, supervisionDecider)

  def ofMultiple(connectionPoolConfig: ScalaPoolConfig,
                 peerConfigs: NonEmptyList[PeerConfig],
                 newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                 supervisionDecider: Option[Supervision.Decider] = None)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): ScalaPool = new ScalaPool(connectionPoolConfig, peerConfigs, newConnection, supervisionDecider)

}

final class ScalaPool private (val connectionPoolConfig: ScalaPoolConfig,
                               val peerConfigs: NonEmptyList[PeerConfig],
                               val newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                               val supervisionDecider: Option[Supervision.Decider] = None)(
    implicit system: ActorSystem,
    scheduler: Scheduler
) extends MemcachedConnectionPool[Task] {

  val DEFAULT_MAX_TOTAL: Int                     = 8
  val DEFAULT_MAX_IDLE_TIME: FiniteDuration      = 5 seconds
  val DEFAULT_VALIDATION_TIMEOUT: FiniteDuration = 3 seconds

  private val client = MemcachedClient()

  private def newPool(peerConfig: PeerConfig): Pool[MemcachedConnection] =
    Pool[MemcachedConnection](
      connectionPoolConfig.sizePerPeer.getOrElse(DEFAULT_MAX_TOTAL),
      factory = { () =>
        newConnection(peerConfig, supervisionDecider)
      },
      referenceType = ReferenceType.Strong,
      maxIdleTime = connectionPoolConfig.maxIdleTime.getOrElse(DEFAULT_MAX_IDLE_TIME),
      reset = { _ =>
        ()
      },
      dispose = { _.shutdown() },
      healthCheck = { con =>
        Await.result(client.version().map(_.nonEmpty).run(con).runAsync,
                     connectionPoolConfig.validationTimeout.getOrElse(DEFAULT_VALIDATION_TIMEOUT))
      }
    )

  private val pools = peerConfigs.toList.map(config => newPool(config))

  pools.foreach(_.fill)

  private val index = new AtomicLong(0L)

  private def getPool = pools(index.getAndIncrement().toInt % pools.size)

  override def withConnectionM[T](reader: ReaderMemcachedConnection[Task, T]): Task[T] = {
    getPool.acquire() { con =>
      reader.run(con)
    }
  }

  override def borrowConnection: Task[MemcachedConnection] = {
    try {
      Task.pure(ScalaPoolConnection(getPool.acquire()))
    } catch {
      case t: Throwable =>
        Task.raiseError(t)
    }
  }

  override def returnConnection(memcachedConnection: MemcachedConnection): Task[Unit] = {
    memcachedConnection match {
      case con: ScalaPoolConnection =>
        try {
          Task.pure(con.underlying.release())
        } catch {
          case t: Throwable =>
            Task.raiseError(t)
        }
      case _ =>
        throw new IllegalArgumentException("Invalid connection class")
    }
  }

  def invalidateConnection(memcachedConnection: MemcachedConnection): Task[Unit] = {
    memcachedConnection match {
      case con: ScalaPoolConnection =>
        try {
          Task.pure(con.underlying.invalidate())
        } catch {
          case t: Throwable =>
            Task.raiseError(t)
        }
      case _ =>
        throw new IllegalArgumentException("Invalid connection class")
    }
  }

  override def numActive: Int = pools.foldLeft(0)(_ + _.live())

  def numIdle: Int = pools.foldLeft(0)(_ + _.size)

  override def clear(): Unit = pools.foreach(_.drain())

  override def dispose(): Unit = pools.foreach(_.close())
}
