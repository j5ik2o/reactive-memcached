package com.github.j5ik2o.reactive.memcached
import akka.actor.ActorSystem
import akka.stream.Supervision
import stormpot.{ Allocator, Slot }

final case class MemcachedConnectionAllocator(
    peerConfig: PeerConfig,
    newConnection: (PeerConfig, Option[Supervision.Decider]) => MemcachedConnection,
    supervisionDecider: Option[Supervision.Decider]
)(implicit system: ActorSystem)
    extends Allocator[MemcachedConnectionPoolable] {

  override def allocate(slot: Slot): MemcachedConnectionPoolable = {
    MemcachedConnectionPoolable(slot, newConnection(peerConfig, supervisionDecider))
  }

  override def deallocate(t: MemcachedConnectionPoolable): Unit = {
    t.close()
  }
}
