package com.github.j5ik2o.reactive.memcached.parser.model

final case class ValueExpr(key: String, flags: Int, expireTime: Long, value: String) extends Expr
