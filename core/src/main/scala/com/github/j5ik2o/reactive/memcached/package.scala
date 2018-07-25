package com.github.j5ik2o.reactive

import cats.data.ReaderT
import monix.eval.Task

package object memcached {

  type ReaderTTask[C, A]                  = ReaderT[Task, C, A]
  type ReaderTTaskMemcachedConnection[A]  = ReaderTTask[MemcachedConnection, A]
  type ReaderMemcachedConnection[M[_], A] = ReaderT[M, MemcachedConnection, A]

}
