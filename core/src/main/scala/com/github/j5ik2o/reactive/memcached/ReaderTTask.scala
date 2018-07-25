package com.github.j5ik2o.reactive.memcached

import cats.data.ReaderT
import monix.eval.Task

object ReaderTTask {

  def pure[C, A](value: A): ReaderTTask[C, A] = ReaderT { _ =>
    Task.pure(value)
  }

  def raiseError[C, A](ex: Throwable): ReaderTTask[C, A] =
    ReaderT { _ =>
      Task.raiseError(ex)
    }

}
