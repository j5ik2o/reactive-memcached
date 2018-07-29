package com.github.j5ik2o.reactive.memcached

import java.nio.charset.StandardCharsets
import java.util.UUID

import com.github.j5ik2o.reactive.memcached.command.CommandRequest
import com.github.j5ik2o.reactive.memcached.util.HashRing
import monix.eval.Task
import org.slf4j.LoggerFactory

final case class HashRingConnection(connectionPools: Seq[MemcachedConnectionPool[Task]], replicas: Int = 2)
    extends MemcachedConnection {

  private val logger = LoggerFactory.getLogger(getClass)

  override def id: UUID = UUID.randomUUID()

  private lazy val hashRingConnections = HashRing(connectionPools.toList, replicas)

  override def peerConfig: Option[PeerConfig] = None

  override def shutdown(): Unit = {
    logger.debug("HashRingConnection#shutdown: start")
    connectionPools.foreach(_.dispose())
    logger.debug("HashRingConnection#shutdown: finished")
  }

  private def key[C <: CommandRequest](cmd: C): Array[Byte] = cmd.key.getBytes(StandardCharsets.UTF_8)

  override def send[C <: CommandRequest](cmd: C): Task[cmd.Response] = {
    logger.debug("HashRingConnection#send: start")
    val connectionPool = hashRingConnections.getNode(key(cmd))
    logger.debug("HashRingConnection#send: connectionPool = {}", connectionPool)
    val result = connectionPool.withConnectionF { con =>
      logger.debug("HashRingConnection#connectionPool#withConnectionF: con = {}", con)
      con.send(cmd)
    }
    logger.debug("HashRingConnection#send: finished")
    result
  }

}
