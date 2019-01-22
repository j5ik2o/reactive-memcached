package com.github.j5ik2o.reactive.memcached
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.stream.Supervision
import cats.data.NonEmptyList
import cn.danielw.fop.{ ObjectFactory, ObjectPool, PoolConfig, Poolable }
import com.github.j5ik2o.reactive.memcached.command.CommandRequest
import monix.eval.Task
import monix.execution.Scheduler

import scala.concurrent.Await
import scala.concurrent.duration._

final case class FOPConnectionWithIndex(index: Int, memcachedConnection: MemcachedConnection)
    extends MemcachedConnection {
  override def id: UUID                                              = memcachedConnection.id
  override def peerConfig: Option[PeerConfig]                        = memcachedConnection.peerConfig
  override def shutdown(): Unit                                      = memcachedConnection.shutdown()
  override def send[C <: CommandRequest](cmd: C): Task[cmd.Response] = memcachedConnection.send(cmd)

}

object FOPPool {

  def ofSingle(connectionPoolConfig: FOPConfig,
               peerConfig: PeerConfig,
               newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
               supervisionDecider: Option[Supervision.Decider] = None)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): FOPPool = new FOPPool(connectionPoolConfig, NonEmptyList.of(peerConfig), newConnection, supervisionDecider)

  def ofMultiple(connectionPoolConfig: FOPConfig,
                 peerConfigs: NonEmptyList[PeerConfig],
                 newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                 supervisionDecider: Option[Supervision.Decider] = None)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): FOPPool = new FOPPool(connectionPoolConfig, peerConfigs, newConnection, supervisionDecider)

  private def createFactory(
      index: Int,
      connectionPoolConfig: FOPConfig,
      peerConfig: PeerConfig,
      newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
      supervisionDecider: Option[Supervision.Decider]
  )(implicit system: ActorSystem, scheduler: Scheduler): ObjectFactory[MemcachedConnection] =
    new ObjectFactory[MemcachedConnection] {
      val client = MemcachedClient()
      override def create(): MemcachedConnection =
        FOPConnectionWithIndex(index, newConnection(peerConfig, supervisionDecider))

      override def destroy(t: MemcachedConnection): Unit = {
        t.shutdown()
      }

      override def validate(t: MemcachedConnection): Boolean = {
        Await.result(client.version().map(_.nonEmpty).run(t).runToFuture,
                     connectionPoolConfig.validationTimeout.getOrElse(3 seconds))
      }
    }

}

final class FOPPool private (val connectionPoolConfig: FOPConfig,
                             val peerConfigs: NonEmptyList[PeerConfig],
                             val newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                             val supervisionDecider: Option[Supervision.Decider] = None)(
    implicit system: ActorSystem,
    scheduler: Scheduler
) extends MemcachedConnectionPool[Task] {

  private val poolConfig = new PoolConfig()
  connectionPoolConfig.maxSizePerPeer.foreach(v => poolConfig.setMaxSize(v))
  connectionPoolConfig.minSizePerPeer.foreach(v => poolConfig.setMinSize(v))
  connectionPoolConfig.maxWaitDuration.foreach(v => poolConfig.setMaxWaitMilliseconds(v.toMillis.toInt))
  connectionPoolConfig.maxIdleDuration.foreach(v => poolConfig.setMaxIdleMilliseconds(v.toMillis.toInt))
  connectionPoolConfig.partitionSizePerPeer.foreach(v => poolConfig.setPartitionSize(v))
  connectionPoolConfig.scavengeInterval.foreach(
    v => poolConfig.setScavengeIntervalMilliseconds(v.toMillis.toInt)
  )
  connectionPoolConfig.scavengeRatio.foreach(poolConfig.setScavengeRatio)

  private val index = new AtomicLong(0L)

  private val objectPools = peerConfigs.toList.zipWithIndex.map {
    case (e, index) =>
      val factory = FOPPool.createFactory(index, connectionPoolConfig, e, newConnection, supervisionDecider)
      new ObjectPool(poolConfig, factory)
  }

  private def getObjectPool = objectPools(index.getAndIncrement().toInt % objectPools.size)

  @SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var", "org.wartremover.warts.Equals"))
  override def withConnectionM[T](reader: ReaderMemcachedConnection[Task, T]): Task[T] = {
    // scalastyle:off
    var p: Poolable[MemcachedConnection] = null
    try {
      p = getObjectPool.borrowObject()
      reader(FOPConnection(p))
    } finally {
      if (p != null)
        p.returnObject()
    }
    // scalastyle:on
  }

  override def borrowConnection: Task[MemcachedConnection] = {
    try {
      val obj = getObjectPool.borrowObject()
      Task.pure(FOPConnection(obj))
    } catch {
      case t: Throwable =>
        Task.raiseError(t)
    }
  }

  override def returnConnection(memcachedConnection: MemcachedConnection): Task[Unit] = {
    memcachedConnection match {
      case con: FOPConnection =>
        try {
          Task.pure(con.underlying.returnObject())
        } catch {
          case t: Throwable =>
            Task.raiseError(t)
        }
      case _ =>
        throw new IllegalArgumentException("Invalid connection class")
    }
  }

  override def numActive: Int = objectPools.foldLeft(0)(_ + _.getSize)

  def numIdle: Int = objectPools.foldLeft(0)(_ + _.getSize)

  override def clear(): Unit = {}

  override def dispose(): Unit = objectPools.foreach(_.shutdown())

}
