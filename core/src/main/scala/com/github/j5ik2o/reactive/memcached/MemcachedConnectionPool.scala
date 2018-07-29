package com.github.j5ik2o.reactive.memcached

import akka.actor.{ ActorSystem, PoisonPill }
import akka.pattern.ask
import akka.routing.{ BalancingPool, Pool, Resizer, RoundRobinPool }
import akka.stream.Supervision
import akka.util.Timeout
import cats.data.{ NonEmptyList, ReaderT }
import cats.{ Monad, MonadError }
import com.github.j5ik2o.reactive.memcached.pool.MemcachedConnectionActor.{ BorrowConnection, ConnectionGotten }
import com.github.j5ik2o.reactive.memcached.pool.{ MemcachedConnectionActor, MemcachedConnectionPoolActor }
import monix.eval.Task
import monix.execution.Scheduler
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

object MemcachedConnectionPool {

  implicit val taskMonadError: MonadError[Task, Throwable] = new MonadError[Task, Throwable] {
    private val taskMonad = implicitly[Monad[Task]]

    override def pure[A](x: A): Task[A] = taskMonad.pure(x)

    override def flatMap[A, B](fa: Task[A])(f: A => Task[B]): Task[B] = taskMonad.flatMap(fa)(f)

    override def tailRecM[A, B](a: A)(f: A => Task[Either[A, B]]): Task[B] = taskMonad.tailRecM(a)(f)

    override def raiseError[A](e: Throwable): Task[A] = Task.raiseError(e)

    override def handleErrorWith[A](fa: Task[A])(f: Throwable => Task[A]): Task[A] = fa.onErrorRecoverWith {
      case t: Throwable => f(t)
    }
  }

  def ofSingleConnection(
      memcachedConnection: MemcachedConnection
  )(implicit system: ActorSystem): MemcachedConnectionPool[Task] =
    new SinglePool(memcachedConnection)

  def ofSingleRoundRobin(
      sizePerPeer: Int,
      peerConfig: PeerConfig,
      newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
      reSizer: Option[Resizer] = None,
      supervisionDecider: Option[Supervision.Decider] = None,
      passingTimeout: FiniteDuration = 5 seconds
  )(implicit system: ActorSystem,
    scheduler: Scheduler,
    ME: MonadError[Task, Throwable]): MemcachedConnectionPool[Task] =
    apply(RoundRobinPool(sizePerPeer, reSizer),
          NonEmptyList.of(peerConfig),
          newConnection,
          supervisionDecider,
          passingTimeout)(
      system,
      scheduler
    )

  def ofMultipleRoundRobin(
      sizePerPeer: Int,
      peerConfigs: NonEmptyList[PeerConfig],
      newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
      reSizer: Option[Resizer] = None,
      supervisionDecider: Option[Supervision.Decider] = None,
      passingTimeout: FiniteDuration = 5 seconds
  )(implicit system: ActorSystem,
    scheduler: Scheduler,
    ME: MonadError[Task, Throwable]): MemcachedConnectionPool[Task] =
    apply(RoundRobinPool(sizePerPeer, reSizer), peerConfigs, newConnection, supervisionDecider, passingTimeout)(
      system,
      scheduler
    )

  def ofSingleBalancing(sizePerPeer: Int,
                        peerConfig: PeerConfig,
                        newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                        supervisionDecider: Option[Supervision.Decider] = None,
                        passingTimeout: FiniteDuration = 5 seconds)(
      implicit system: ActorSystem,
      scheduler: Scheduler,
      ME: MonadError[Task, Throwable]
  ): MemcachedConnectionPool[Task] =
    apply(BalancingPool(sizePerPeer), NonEmptyList.of(peerConfig), newConnection, supervisionDecider, passingTimeout)(
      system,
      scheduler
    )

  def ofMultipleBalancing(sizePerPeer: Int,
                          peerConfigs: NonEmptyList[PeerConfig],
                          newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                          supervisionDecider: Option[Supervision.Decider] = None,
                          passingTimeout: FiniteDuration = 5 seconds)(
      implicit system: ActorSystem,
      scheduler: Scheduler,
      ME: MonadError[Task, Throwable]
  ): MemcachedConnectionPool[Task] =
    apply(BalancingPool(sizePerPeer), peerConfigs, newConnection, supervisionDecider, passingTimeout)(
      system,
      scheduler
    )

  def apply(pool: Pool,
            peerConfigs: NonEmptyList[PeerConfig],
            newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
            supervisionDecider: Option[Supervision.Decider] = None,
            passingTimeout: FiniteDuration = 3 seconds)(
      implicit system: ActorSystem,
      scheduler: Scheduler
  ): MemcachedConnectionPool[Task] =
    new AkkaPool(pool, peerConfigs, newConnection, supervisionDecider, passingTimeout)(system, scheduler)

  private class AkkaPool(pool: Pool,
                         val peerConfigs: NonEmptyList[PeerConfig],
                         newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
                         supervisionDecider: Option[Supervision.Decider] = None,
                         passingTimeout: FiniteDuration = 3 seconds)(implicit system: ActorSystem, scheduler: Scheduler)
      extends MemcachedConnectionPool[Task]() {

    private val connectionPoolActor =
      system.actorOf(
        MemcachedConnectionPoolActor.props(
          pool,
          peerConfigs.map(v => MemcachedConnectionActor.props(v, newConnection, supervisionDecider, passingTimeout))
        )
      )

    private implicit val to: Timeout = passingTimeout

    override def withConnectionM[T](reader: ReaderMemcachedConnection[Task, T]): Task[T] = {
      borrowConnection.flatMap { con =>
        reader.run(con).doOnFinish { _ =>
          returnConnection(con)
        }
      }
    }

    override def borrowConnection: Task[MemcachedConnection] = Task.deferFutureAction { implicit ec =>
      (connectionPoolActor ? BorrowConnection)
        .mapTo[ConnectionGotten]
        .map { v =>
          logger.debug("reply = {}", v)
          v.memcachedConnection
        }(ec)
    }

    override def returnConnection(memcachedConnection: MemcachedConnection): Task[Unit] = {
      Task.pure(())
    }

    override def numActive: Int = pool.nrOfInstances(system) * peerConfigs.size

    override def clear(): Unit = {}

    override def dispose(): Unit = { connectionPoolActor ! PoisonPill }
  }

  private class SinglePool(memcachedConnection: MemcachedConnection)(implicit system: ActorSystem)
      extends MemcachedConnectionPool[Task]() {

    override def peerConfigs: NonEmptyList[PeerConfig] =
      memcachedConnection.peerConfig.map(v => NonEmptyList.of(v)).getOrElse(throw new NoSuchElementException)

    override def withConnectionM[T](reader: ReaderMemcachedConnection[Task, T]): Task[T] = reader(memcachedConnection)

    override def borrowConnection: Task[MemcachedConnection] = Task.pure(memcachedConnection)

    override def returnConnection(memcachedConnection: MemcachedConnection): Task[Unit] = Task.pure(())

    def invalidateConnection(memcachedConnection: MemcachedConnection): Task[Unit] = Task.pure(())

    override def numActive: Int = 1

    override def clear(): Unit = {}

    override def dispose(): Unit = memcachedConnection.shutdown()

  }
}

abstract class MemcachedConnectionPool[M[_]] {

  def peerConfigs: NonEmptyList[PeerConfig]

  protected val logger = LoggerFactory.getLogger(getClass)

  def withConnectionF[T](f: MemcachedConnection => M[T]): M[T] = withConnectionM(ReaderT(f))

  def withConnectionM[T](reader: ReaderMemcachedConnection[M, T]): M[T]

  def borrowConnection: M[MemcachedConnection]

  def returnConnection(memcachedConnection: MemcachedConnection): M[Unit]

  def numActive: Int

  def clear(): Unit

  def dispose(): Unit
}
