package com.github.j5ik2o.reactive.memcached.command

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.parser.StringParsers.storageCommandResponse
import com.github.j5ik2o.reactive.memcached.parser.model.{ EmptyExpr, Expr }
import fastparse.all._

import scala.concurrent.duration.Duration

abstract class StorageRequest(val id: UUID,
                              val key: String,
                              val flags: Int,
                              val expireDuration: Duration,
                              val value: String,
                              val noReply: Boolean = false)
    extends CommandRequest
    with StringParsersSupport {

  protected val commandName: String
  override val isMasterOnly: Boolean = true

  private def toSeconds: Long = if (expireDuration.isFinite()) expireDuration.toSeconds else 0
  private def bytes: Int      = value.getBytes(StandardCharsets.UTF_8).length

  override def asString: String = s"$commandName $key $flags $toSeconds $bytes\r\n$value"

  override protected def responseParser: P[Expr] =
    if (noReply) P(End).map(_ => EmptyExpr) else P(storageCommandResponse)

}
