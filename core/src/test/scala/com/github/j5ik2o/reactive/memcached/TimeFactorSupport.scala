package com.github.j5ik2o.reactive.memcached

import akka.testkit.TestKit

trait TimeFactorSupport { self: TestKit =>

  lazy val timeFactor: Double = testKitSettings.TestTimeFactor

}
