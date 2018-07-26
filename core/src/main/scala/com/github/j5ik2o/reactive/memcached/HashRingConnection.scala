package com.github.j5ik2o.reactive.memcached
import java.nio.charset.StandardCharsets
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.command.CommandRequestBase
import com.github.j5ik2o.reactive.memcached.util.HashRing
import monix.eval.Task

final case class HashRingConnection(connectionPools: Seq[MemcachedConnectionPool[Task]], replicas: Int = 2)
    extends MemcachedConnection {
  override def id: UUID = UUID.randomUUID()

  private lazy val hashRingConnections = HashRing(connectionPools.toList, replicas)

  override def peerConfig: Option[PeerConfig] = None

  override def shutdown(): Unit = {
    connectionPools.foreach(_.dispose())
  }

  private def key[C <: CommandRequestBase](cmd: C): Array[Byte] = cmd.key.getBytes(StandardCharsets.UTF_8)

  override def send[C <: CommandRequestBase](cmd: C): Task[cmd.Response] =
    hashRingConnections.getNode(key(cmd)).withConnectionF(_.send(cmd))

}
