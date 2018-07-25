package com.github.j5ik2o.reactive.memcached

@SuppressWarnings(Array("org.wartremover.warts.Null"))
abstract class MemcachedBaseException(message: Option[String], cause: Option[Throwable])
    extends Exception(message.orNull, cause.orNull)
