package com.github.j5ik2o.reactive.memcached

import cats.MonadError
import cats.implicits._
import monix.eval.Task

abstract class ConnectionAutoClose[M[_], A](val pool: MemcachedConnectionPool[M])(
    implicit ME: MonadError[M, Throwable]
) {
  self =>

  protected def process(res: MemcachedConnection): M[A]

  def run(): M[A] = pool.borrowConnection.flatMap { res =>
    process(res)
      .flatMap { v =>
        pool.returnConnection(res).map(_ => v)
      }
      .recoverWith {
        case t: Throwable =>
          pool.returnConnection(res).flatMap(_ => ME.raiseError(t))
      }
  }

  def flatMap[B](f: A => ConnectionAutoClose[M, B]): ConnectionAutoClose[M, B] =
    new ConnectionAutoClose[M, B](pool) {
      override protected def process(res: MemcachedConnection): M[B] = {
        self.process(res).flatMap(v => f(v).process(res))
      }
    }

  def map[B](f: A => B): ConnectionAutoClose[M, B] =
    new ConnectionAutoClose[M, B](pool) {
      override protected def process(res: MemcachedConnection): M[B] = {
        self.process(res).map(v => f(v))
      }
    }

}

object ConnectionAutoClose {

  type TransactionTask[A] = ConnectionAutoClose[Task, A]

  def raiseError[M[_], A](
      pool: MemcachedConnectionPool[M]
  )(ex: Throwable)(implicit ME: MonadError[M, Throwable]): ConnectionAutoClose[M, A] =
    new ConnectionAutoClose[M, A](pool) {
      override protected def process(res: MemcachedConnection): M[A] = ME.raiseError(ex)
    }

  def pure[M[_], A](
      pool: MemcachedConnectionPool[M]
  )(a: MemcachedConnection => M[A])(implicit ME: MonadError[M, Throwable]): ConnectionAutoClose[M, A] =
    new ConnectionAutoClose[M, A](pool) {
      override protected def process(res: MemcachedConnection) = a(res)
    }

  def apply[M[_], A](
      pool: MemcachedConnectionPool[M]
  )(a: MemcachedConnection => M[A])(implicit ME: MonadError[M, Throwable]): ConnectionAutoClose[M, A] = pure(pool)(a)

  def unapply[A](arg: ConnectionAutoClose[Task, A]): Option[MemcachedConnectionPool[Task]] =
    Some(arg.pool)

}
