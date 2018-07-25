package com.github.j5ik2o.reactive.memcached.command

import java.util.UUID

import com.github.j5ik2o.reactive.memcached.parser.model.Expr

case class QuitRequest(id: UUID) extends CommandRequest with StringParsersSupport {

  override type Response = this.type

  override val isMasterOnly: Boolean = false

  override def asString: String = "quit"

  override protected def responseParser: P[Expr] = ???
  override protected def parseResponse: Handler  = ???
}
