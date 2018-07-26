package com.github.j5ik2o.reactive.memcached
import stormpot.{ Poolable, Slot }

final case class MemcachedConnectionPoolable(slot: Slot, memcachedConnection: MemcachedConnection) extends Poolable {
  override def release(): Unit = {
    slot.release(this)
  }
  def expire(): Unit = slot.expire(this)
  def close(): Unit  = memcachedConnection.shutdown()
}
