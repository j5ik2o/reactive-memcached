package com.github.j5ik2o.reactive.memcached

final case class MemcachedIOException(errorType: ErrorType, message: Option[String], cause: Option[Throwable] = None)
    extends MemcachedBaseException(message, cause)
