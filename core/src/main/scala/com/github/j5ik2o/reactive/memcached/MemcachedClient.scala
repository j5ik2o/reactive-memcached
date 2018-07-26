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
          flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(SetRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case SetExisted(_, _)    => ReaderTTask.pure(0)
      case SetNotFounded(_, _) => ReaderTTask.pure(0)
      case SetNotStored(_, _)  => ReaderTTask.pure(0)
      case SetSucceeded(_, _)  => ReaderTTask.pure(1)
      case SetFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def add(key: String,
          value: String,
          expire: Duration = Duration.Inf,
          flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(AddRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case AddExisted(_, _)    => ReaderTTask.pure(0)
      case AddNotFounded(_, _) => ReaderTTask.pure(0)
      case AddNotStored(_, _)  => ReaderTTask.pure(0)
      case AddSucceeded(_, _)  => ReaderTTask.pure(1)
      case AddFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def append(key: String,
             value: String,
             expire: Duration = Duration.Inf,
             flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(AppendRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case AppendExisted(_, _)    => ReaderTTask.pure(0)
      case AppendNotFounded(_, _) => ReaderTTask.pure(0)
      case AppendNotStored(_, _)  => ReaderTTask.pure(0)
      case AppendSucceeded(_, _)  => ReaderTTask.pure(1)
      case AppendFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def prepend(key: String,
              value: String,
              expire: Duration = Duration.Inf,
              flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(PrependRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case PrependExisted(_, _)    => ReaderTTask.pure(0)
      case PrependNotFounded(_, _) => ReaderTTask.pure(0)
      case PrependNotStored(_, _)  => ReaderTTask.pure(0)
      case PrependSucceeded(_, _)  => ReaderTTask.pure(1)
      case PrependFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def replace(key: String,
              value: String,
              expire: Duration = Duration.Inf,
              flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(ReplaceRequest(UUID.randomUUID(), key, flags, expire, value)).flatMap {
      case ReplaceExisted(_, _)    => ReaderTTask.pure(0)
      case ReplaceNotFounded(_, _) => ReaderTTask.pure(0)
      case ReplaceNotStored(_, _)  => ReaderTTask.pure(0)
      case ReplaceSucceeded(_, _)  => ReaderTTask.pure(1)
      case ReplaceFailed(_, _, ex) => ReaderTTask.raiseError(ex)
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
