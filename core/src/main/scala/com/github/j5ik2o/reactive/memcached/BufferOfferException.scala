package com.github.j5ik2o.reactive.memcached

final case class BufferOfferException(message: String, cause: Option[Throwable] = None)
    extends MemcachedBaseException(Some(message), cause)
