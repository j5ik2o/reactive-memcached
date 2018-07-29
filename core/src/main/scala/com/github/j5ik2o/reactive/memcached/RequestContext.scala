package com.github.j5ik2o.reactive.memcached

import java.time.ZonedDateTime
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.command.{ CommandRequest, CommandResponse }

import scala.concurrent.Promise

final case class RequestContext(commandRequest: CommandRequest,
                                promise: Promise[CommandResponse],
                                requestAt: ZonedDateTime) {
  val id: UUID                     = commandRequest.id
  val commandRequestString: String = commandRequest.asString
}
