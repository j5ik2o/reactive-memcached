package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

trait CommandResponse {
  val id: UUID
  val requestId: UUID
}
