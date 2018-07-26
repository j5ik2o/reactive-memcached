package com.github.j5ik2o.reactive.memcached
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorSystem
import akka.event.{ LogSource, Logging }
import akka.stream.Supervision
import cats.data.NonEmptyList
import com.github.j5ik2o.reactive.memcached.CommonsPool.MemcachedConnectionPoolFactory
import com.github.j5ik2o.reactive.memcached.command.CommandRequestBase
import monix.eval.Task
import monix.execution.Scheduler
import org.apache.commons.pool2.{ BasePooledObjectFactory, PooledObject }
import org.apache.commons.pool2.impl.{ DefaultPooledObject, GenericObjectPool, GenericObjectPoolConfig }

import scala.concurrent.duration._

final case class MemcachedConnectionPoolable(index: Int, redisConnection: MemcachedConnection)
    extends MemcachedConnection {
  override def id: UUID                                                  = redisConnection.id
  override def peerConfig: Option[PeerConfig]                            = redisConnection.peerConfig
  override def shutdown(): Unit                                          = redisConnection.shutdown()
  override def send[C <: CommandRequestBase](cmd: C): Task[cmd.Response] = redisConnection.send(cmd)

}
object CommonsPool {

  def ofSingle(connectionPoolConfig: CommonsPoolConfig,
               peerConfig: PeerConfig,
               newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
               supervisionDecider: Option[Supervision.Decider] = None,
               validationTimeout: FiniteDuration = 3 seconds)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): CommonsPool =
    new CommonsPool(connectionPoolConfig,
                    NonEmptyList.of(peerConfig),
                    newConnection,
                    supervisionDecider,
                    validationTimeout)

  def ofMultiple(connectionPoolConfig: CommonsPoolConfig,
                 peerConfigs: NonEmptyList[PeerConfig],
                 newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                 supervisionDecider: Option[Supervision.Decider] = None,
                 validationTimeout: FiniteDuration = 3 seconds)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): CommonsPool =
    new CommonsPool(connectionPoolConfig, peerConfigs, newConnection, supervisionDecider, validationTimeout)

  private class MemcachedConnectionPoolFactory(
      index: Int,
      peerConfig: PeerConfig,
      supervisionDecider: Option[Supervision.Decider],
      newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
      validationTimeout: FiniteDuration
  )(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ) extends BasePooledObjectFactory[MemcachedConnectionPoolable] {

    implicit val logSource: LogSource[MemcachedConnectionPoolFactory] = new LogSource[MemcachedConnectionPoolFactory] {
      override def genString(o: MemcachedConnectionPoolFactory): String  = o.getClass.getName
      override def getClazz(o: MemcachedConnectionPoolFactory): Class[_] = o.getClass
    }

    val log = Logging(system, this)

    private val redisClient = MemcachedClient()

    override def create(): MemcachedConnectionPoolable =
      MemcachedConnectionPoolable(index, newConnection(peerConfig, supervisionDecider))

    override def destroyObject(p: PooledObject[MemcachedConnectionPoolable]): Unit =
      p.getObject.shutdown()

    override def wrap(t: MemcachedConnectionPoolable): PooledObject[MemcachedConnectionPoolable] =
      new DefaultPooledObject(t)

    override def validateObject(p: PooledObject[MemcachedConnectionPoolable]): Boolean = {
      true
      // val connection = p.getObject
      // redisClient.validate(validationTimeout).run(connection)
    }

  }
}
class CommonsPool(val connectionPoolConfig: CommonsPoolConfig,
                  val peerConfigs: NonEmptyList[PeerConfig],
                  val newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                  val supervisionDecider: Option[Supervision.Decider] = None,
                  val validationTimeout: FiniteDuration = 3 seconds)(
    implicit system: ActorSystem,
    scheduler: Scheduler
) extends MemcachedConnectionPool[Task] {
  private val abandonedConfig: org.apache.commons.pool2.impl.AbandonedConfig =
    new org.apache.commons.pool2.impl.AbandonedConfig()

  connectionPoolConfig.abandonedConfig.foreach { v =>
    v.logAbandoned.foreach(abandonedConfig.setLogAbandoned)
    v.removeAbandonedOnBorrow.foreach(abandonedConfig.setRemoveAbandonedOnBorrow)
    v.removeAbandonedOnMaintenance.foreach(abandonedConfig.setRemoveAbandonedOnMaintenance)
    v.logWriter.foreach(abandonedConfig.setLogWriter)
    v.removeAbandonedTimeout.foreach(v => abandonedConfig.setRemoveAbandonedTimeout(v.toSeconds.toInt))
    v.requireFullStackTrace.foreach(abandonedConfig.setRequireFullStackTrace)
    v.useUsageTracking.foreach(abandonedConfig.setUseUsageTracking)
  }

  private val underlyingPoolConfig: GenericObjectPoolConfig[MemcachedConnectionPoolable] =
    new GenericObjectPoolConfig[MemcachedConnectionPoolable]()

  connectionPoolConfig.lifo.foreach(underlyingPoolConfig.setLifo)
  connectionPoolConfig.fairness.foreach(underlyingPoolConfig.setFairness)
  connectionPoolConfig.maxWaitMillis.foreach { v =>
    if (v.isFinite())
      underlyingPoolConfig.setMaxWaitMillis(v.toMillis)
    else
      underlyingPoolConfig.setMaxWaitMillis(-1L)
  }
  connectionPoolConfig.minEvictableIdleTime.foreach { v =>
    if (v.isFinite())
      underlyingPoolConfig.setMinEvictableIdleTimeMillis(v.toMillis)
    else
      underlyingPoolConfig.setMinEvictableIdleTimeMillis(-1L)
  }
  connectionPoolConfig.evictorShutdownTimeout.foreach { v =>
    if (v.isFinite())
      underlyingPoolConfig.setEvictorShutdownTimeoutMillis(v.toMillis)
    else
      underlyingPoolConfig.setEvictorShutdownTimeoutMillis(-1L)
  }
  connectionPoolConfig.softMinEvictableIdleTime.foreach { v =>
    if (v.isFinite())
      underlyingPoolConfig.setSoftMinEvictableIdleTimeMillis(v.toMillis)
    else
      underlyingPoolConfig.setSoftMinEvictableIdleTimeMillis(-1L)
  }

  connectionPoolConfig.numTestsPerEvictionRun.foreach(underlyingPoolConfig.setNumTestsPerEvictionRun)
  connectionPoolConfig.evictionPolicy.foreach(underlyingPoolConfig.setEvictionPolicy)
  connectionPoolConfig.evictionPolicyClassName.foreach(underlyingPoolConfig.setEvictionPolicyClassName)

  connectionPoolConfig.testOnCreate.foreach(underlyingPoolConfig.setTestOnCreate)
  connectionPoolConfig.testOnBorrow.foreach(underlyingPoolConfig.setTestOnBorrow)
  connectionPoolConfig.testOnReturn.foreach(underlyingPoolConfig.setTestOnReturn)
  connectionPoolConfig.testWhileIdle.foreach(underlyingPoolConfig.setTestWhileIdle)
  connectionPoolConfig.timeBetweenEvictionRuns.foreach { v =>
    if (v.isFinite())
      underlyingPoolConfig.setTimeBetweenEvictionRunsMillis(v.toMillis)
    else
      underlyingPoolConfig.setTimeBetweenEvictionRunsMillis(-1L)
  }
  connectionPoolConfig.blockWhenExhausted.foreach(underlyingPoolConfig.setBlockWhenExhausted)
  connectionPoolConfig.jmxEnabled.foreach(underlyingPoolConfig.setJmxEnabled)
  connectionPoolConfig.jmxNamePrefix.foreach(underlyingPoolConfig.setJmxNamePrefix)
  connectionPoolConfig.jmxNameBase.foreach(underlyingPoolConfig.setJmxNameBase)

  connectionPoolConfig.sizePerPeer.foreach(v => underlyingPoolConfig.setMaxTotal(v))
  connectionPoolConfig.maxIdlePerPeer.foreach(v => underlyingPoolConfig.setMaxIdle(v))
  connectionPoolConfig.minIdlePerPeer.foreach(v => underlyingPoolConfig.setMinIdle(v))

  private def underlyingConnectionPool(index: Int,
                                       peerConfig: PeerConfig): GenericObjectPool[MemcachedConnectionPoolable] =
    new GenericObjectPool[MemcachedConnectionPoolable](
      new MemcachedConnectionPoolFactory(index, peerConfig, supervisionDecider, newConnection, validationTimeout),
      underlyingPoolConfig
    )

  private val underlyingConnectionPools: Seq[GenericObjectPool[MemcachedConnectionPoolable]] = {
    val results = peerConfigs.toList.zipWithIndex.map { case (e, index) => underlyingConnectionPool(index, e) }
    if (connectionPoolConfig.abandonedConfig.nonEmpty)
      results.foreach(_.setAbandonedConfig(abandonedConfig))
    results
  }

  private val index = new AtomicLong(1L)

  private def getUnderlyingConnectionPool: GenericObjectPool[MemcachedConnectionPoolable] = {
    val idx = index.getAndIncrement().toInt % underlyingConnectionPools.size
    underlyingConnectionPools(idx)
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null", "org.wartremover.warts.Var", "org.wartremover.warts.Equals"))
  override def withConnectionM[T](
      reader: ReaderMemcachedConnection[Task, T]
  ): Task[T] = {
    // scalastyle:off
    var con: MemcachedConnectionPoolable = null
    try {
      con = getUnderlyingConnectionPool.borrowObject()
      reader(con)
    } finally {
      if (con != null)
        underlyingConnectionPools(con.index).returnObject(con)
    }
    // scalastyle:on
  }

  override def borrowConnection: Task[MemcachedConnection] =
    try {
      Task.pure(getUnderlyingConnectionPool.borrowObject())
    } catch {
      case t: Throwable =>
        Task.raiseError(t)
    }

  override def returnConnection(
      memcachedConnection: MemcachedConnection
  ): Task[Unit] =
    try {
      memcachedConnection match {
        case c: MemcachedConnectionPoolable =>
          Task.pure(underlyingConnectionPools(c.index).returnObject(c))
        case _ =>
          throw new IllegalArgumentException("Invalid connection class")
      }
    } catch {
      case t: Throwable =>
        Task.raiseError(t)
    }

  def invalidateConnection(memcachedConnection: MemcachedConnection): Task[Unit] =
    try {
      memcachedConnection match {
        case c: MemcachedConnectionPoolable =>
          Task.pure(underlyingConnectionPools(c.index).invalidateObject(c))
        case _ =>
          throw new IllegalArgumentException("Invalid connection class")
      }
    } catch {
      case t: Throwable =>
        Task.raiseError(t)
    }

  override def numActive: Int = underlyingConnectionPools.foldLeft(0)((r, e) => r + e.getNumActive)

  def numIdle: Int = underlyingConnectionPools.foldLeft(0)((r, e) => r + e.getNumIdle)

  override def clear(): Unit = underlyingConnectionPools.foreach(_.clear())

  override def dispose(): Unit = underlyingConnectionPools.foreach(_.close())
}
