package com.github.j5ik2o.reactive.memcached

import java.util.UUID

import akka.actor.ActorSystem
import cats.data.{ NonEmptyList, ReaderT }
import com.github.j5ik2o.reactive.memcached.command._

import scala.concurrent.duration.Duration

class MemcachedClient(implicit system: ActorSystem) {

  def send[C <: CommandRequestBase](cmd: C): ReaderTTaskMemcachedConnection[cmd.Response] = ReaderT(_.send(cmd))

  def set(key: String,
          value: String,
          expire: Duration = Duration.Inf,
          flags: Int = 0): ReaderTTaskMemcachedConnection[Unit] =
    send(SetRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case SetSucceeded(_, _)  => ReaderTTask.pure(())
      case SetFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def get(keys: NonEmptyList[String]): ReaderTTaskMemcachedConnection[Option[Seq[ValueDesc]]] =
    send(GetRequest(UUID.randomUUID(), keys)).flatMap {
      case GetSucceeded(_, _, result) => ReaderTTask.pure(result)
      case GetFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def delete(key: String): ReaderTTaskMemcachedConnection[Int] =
    send(DeleteRequest(UUID.randomUUID(), key)).flatMap {
      case DeleteNotFound(_, _)   => ReaderTTask.pure(0)
      case DeleteSucceeded(_, _)  => ReaderTTask.pure(1)
      case DeleteFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

}
