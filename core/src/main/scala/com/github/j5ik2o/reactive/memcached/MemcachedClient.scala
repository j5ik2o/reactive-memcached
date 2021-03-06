package com.github.j5ik2o.reactive.memcached

import java.util.UUID

import akka.actor.ActorSystem
import cats.Show
import cats.data.ReaderT
import com.github.j5ik2o.reactive.memcached.command._

import scala.concurrent.duration.Duration

object MemcachedClient {

  def apply()(implicit system: ActorSystem): MemcachedClient = new MemcachedClient()

}

final class MemcachedClient()(implicit system: ActorSystem) {

  def send[C <: CommandRequest](cmd: C): ReaderTTaskMemcachedConnection[cmd.Response] = ReaderT(_.send(cmd))

  def set[A: Show](key: String,
                   value: A,
                   expireDuration: Duration = Duration.Inf,
                   flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(SetRequest(UUID.randomUUID(), key, value, expireDuration, flags)).flatMap {
      case SetExisted(_, _)    => ReaderTTask.pure(0)
      case SetNotFounded(_, _) => ReaderTTask.pure(0)
      case SetNotStored(_, _)  => ReaderTTask.pure(0)
      case SetSucceeded(_, _)  => ReaderTTask.pure(1)
      case SetFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def add[A: Show](key: String,
                   value: A,
                   expireDuration: Duration = Duration.Inf,
                   flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(AddRequest(UUID.randomUUID(), key, value, expireDuration, flags)).flatMap {
      case AddExisted(_, _)    => ReaderTTask.pure(0)
      case AddNotFounded(_, _) => ReaderTTask.pure(0)
      case AddNotStored(_, _)  => ReaderTTask.pure(0)
      case AddSucceeded(_, _)  => ReaderTTask.pure(1)
      case AddFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def append[A: Show](key: String,
                      value: A,
                      expireDuration: Duration = Duration.Inf,
                      flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(AppendRequest(UUID.randomUUID(), key, value, expireDuration, flags)).flatMap {
      case AppendExisted(_, _)    => ReaderTTask.pure(0)
      case AppendNotFounded(_, _) => ReaderTTask.pure(0)
      case AppendNotStored(_, _)  => ReaderTTask.pure(0)
      case AppendSucceeded(_, _)  => ReaderTTask.pure(1)
      case AppendFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def prepend[A: Show](key: String,
                       value: A,
                       expireDuration: Duration = Duration.Inf,
                       flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(PrependRequest(UUID.randomUUID(), key, value, expireDuration, flags)).flatMap {
      case PrependExisted(_, _)    => ReaderTTask.pure(0)
      case PrependNotFounded(_, _) => ReaderTTask.pure(0)
      case PrependNotStored(_, _)  => ReaderTTask.pure(0)
      case PrependSucceeded(_, _)  => ReaderTTask.pure(1)
      case PrependFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def replace[A: Show](key: String,
                       value: A,
                       expireDuration: Duration = Duration.Inf,
                       flags: Int = 0): ReaderTTaskMemcachedConnection[Int] =
    send(ReplaceRequest(UUID.randomUUID(), key, value, expireDuration, flags)).flatMap {
      case ReplaceExisted(_, _)    => ReaderTTask.pure(0)
      case ReplaceNotFounded(_, _) => ReaderTTask.pure(0)
      case ReplaceNotStored(_, _)  => ReaderTTask.pure(0)
      case ReplaceSucceeded(_, _)  => ReaderTTask.pure(1)
      case ReplaceFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def cas[A: Show](
      key: String,
      value: A,
      casUnique: Long,
      expireDurationDuration: Duration = Duration.Inf,
      flags: Int = 0
  ): ReaderTTaskMemcachedConnection[Int] =
    send(CasRequest(UUID.randomUUID(), key, value, casUnique, expireDurationDuration, flags)).flatMap {
      case CasExisted(_, _)    => ReaderTTask.pure(0)
      case CasNotFounded(_, _) => ReaderTTask.pure(0)
      case CasNotStored(_, _)  => ReaderTTask.pure(0)
      case CasSucceeded(_, _)  => ReaderTTask.pure(1)
      case CasFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def increment(key: String, value: Long): ReaderTTaskMemcachedConnection[Option[Long]] =
    send(IncrementRequest(UUID.randomUUID(), key, value)).flatMap {
      case IncrementNotFound(_, _)          => ReaderTTask.pure(None)
      case IncrementSucceeded(_, _, result) => ReaderTTask.pure(Some(result))
      case IncrementFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def decrement(key: String, value: Long): ReaderTTaskMemcachedConnection[Option[Long]] =
    send(DecrementRequest(UUID.randomUUID(), key, value)).flatMap {
      case DecrementNotFound(_, _)          => ReaderTTask.pure(None)
      case DecrementSucceeded(_, _, result) => ReaderTTask.pure(Some(result))
      case DecrementFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def get(key: String): ReaderTTaskMemcachedConnection[Option[ValueDesc]] =
    send(GetRequest(UUID.randomUUID(), key)).flatMap {
      case GetSucceeded(_, _, result) => ReaderTTask.pure(result)
      case GetFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def gets(key: String): ReaderTTaskMemcachedConnection[Option[ValueDesc]] =
    send(GetsRequest(UUID.randomUUID(), key)).flatMap {
      case GetsSucceeded(_, _, result) => ReaderTTask.pure(result)
      case GetsFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def delete(key: String): ReaderTTaskMemcachedConnection[Int] =
    send(DeleteRequest(UUID.randomUUID(), key)).flatMap {
      case DeleteNotFound(_, _)   => ReaderTTask.pure(0)
      case DeleteSucceeded(_, _)  => ReaderTTask.pure(1)
      case DeleteFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def touch(key: String, expireDuration: Duration): ReaderTTaskMemcachedConnection[Int] =
    send(TouchRequest(UUID.randomUUID(), key, expireDuration)).flatMap {
      case TouchNotFounded(_, _) => ReaderTTask.pure(0)
      case TouchSucceeded(_, _)  => ReaderTTask.pure(1)
      case TouchFailed(_, _, ex) => ReaderTTask.raiseError(ex)
    }

  def gat(key: String, expireDuration: Duration): ReaderTTaskMemcachedConnection[Option[ValueDesc]] =
    send(GatRequest(UUID.randomUUID(), key, expireDuration)).flatMap {
      case GatSucceeded(_, _, result) => ReaderTTask.pure(result)
      case GatFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def gats(key: String, expireDuration: Duration): ReaderTTaskMemcachedConnection[Option[ValueDesc]] =
    send(GatsRequest(UUID.randomUUID(), key, expireDuration)).flatMap {
      case GatsSucceeded(_, _, result) => ReaderTTask.pure(result)
      case GatsFailed(_, _, ex)        => ReaderTTask.raiseError(ex)
    }

  def version(): ReaderTTaskMemcachedConnection[String] =
    send(VersionRequest(UUID.randomUUID())).flatMap {
      case VersionSucceeded(_, _, value) => ReaderTTask.pure(value)
      case VersionFailed(_, _, ex)       => ReaderTTask.raiseError(ex)
    }

}
