val compileScalaStyle = taskKey[Unit]("compileScalaStyle")

lazy val scalaStyleSettings = Seq(
  (scalastyleConfig in Compile) := file("scalastyle-config.xml"),
  compileScalaStyle := scalastyle.in(Compile).toTask("").value,
  (compile in Compile) := (compile in Compile).dependsOn(compileScalaStyle).value
)

val coreSettings = Seq(
  sonatypeProfileName := "com.github.j5ik2o",
  organization := "com.github.j5ik2o",
  scalaVersion := "2.11.11",
  crossScalaVersions ++= Seq("2.11.11", "2.12.6"),
  scalacOptions ++= {
    Seq(
      "-feature",
      "-deprecation",
      "-unchecked",
      "-encoding",
      "UTF-8",
      "-language:existentials",
      "-language:implicitConversions",
      "-language:postfixOps",
      "-language:higherKinds"
    ) ++ {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2L, scalaMajor)) if scalaMajor == 12 =>
          Seq.empty
        case Some((2L, scalaMajor)) if scalaMajor <= 11 =>
          Seq(
            "-Yinline-warnings"
          )
      }
    }
  },
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo in ThisBuild := sonatypePublishTo.value,
  credentials := {
    val ivyCredentials = (baseDirectory in LocalRootProject).value / ".credentials"
    Credentials(ivyCredentials) :: Nil
  },
  scalafmtOnCompile in ThisBuild := true,
  scalafmtTestOnCompile in ThisBuild := true,
  resolvers += Resolver.bintrayRepo("danslapman", "maven"),
  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
  libraryDependencies ++= Seq(
    "io.monix"       %% "monix"          % "3.0.0-RC1",
    "org.typelevel"  %% "cats-core"      % "1.1.0",
    "org.typelevel"  %% "cats-free"      % "1.1.0",
    "com.beachape"   %% "enumeratum"     % "1.5.13",
    "org.slf4j"      % "slf4j-api"       % "1.7.25",
    "danslapman"     %% "cats-conts"     % "0.4",
    "org.scalatest"  %% "scalatest"      % "3.0.5" % Test,
    "org.scalacheck" %% "scalacheck"     % "1.14.0" % Test,
    "ch.qos.logback" % "logback-classic" % "1.2.3" % Test
  ),
  //  Global / concurrentRestrictions += Tags.limit(Tags.Test, 1),
  parallelExecution in Test := false,
  wartremoverErrors ++= Warts.allBut(Wart.Any,
                                     Wart.Throw,
                                     Wart.Nothing,
                                     Wart.Product,
                                     Wart.NonUnitStatements,
                                     Wart.DefaultArguments,
                                     Wart.ImplicitParameter,
                                     Wart.StringPlusAny,
                                     Wart.Overloading),
  wartremoverExcluded += baseDirectory.value / "src" / "test" / "scala"
) ++ scalaStyleSettings

val akkaVersion = "2.5.11"

lazy val core = (project in file("core")).settings(
  coreSettings ++ Seq(
    name := "reactive-memcached-core",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor"     % akkaVersion,
      "com.typesafe.akka" %% "akka-testkit"   % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream"    % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j"     % akkaVersion,
      "com.lihaoyi"       %% "fastparse"      % "1.0.0",
      "com.lihaoyi"       %% "fastparse-byte" % "1.0.0",
      "com.google.guava"  % "guava"           % "25.1-jre" % Test,
      "commons-io"        % "commons-io"      % "2.6" % Test
    )
  )
)

lazy val `pool-commons` = (project in file("pool-commons"))
  .settings(
    coreSettings ++ Seq(
      name := "reactive-memcached-pool-commons",
      libraryDependencies ++= Seq(
        "org.apache.commons" % "commons-pool2" % "2.6.0"
      )
    )
  )
  .dependsOn(core % "compile;test->test")

lazy val `pool-fop` = (project in file("pool-fop"))
  .settings(
    coreSettings ++ Seq(
      name := "reactive-memcached-pool-fop",
      libraryDependencies ++= Seq(
        "cn.danielw" % "fast-object-pool" % "2.1.0"
      )
    )
  )
  .dependsOn(core % "compile;test->test")

lazy val `pool-stormpot` = (project in file("pool-stormpot"))
  .settings(
    coreSettings ++ Seq(
      name := "reactive-memcached-pool-stormpot",
      libraryDependencies ++= Seq(
        "com.github.chrisvest" % "stormpot" % "2.4"
      )
    )
  )
  .dependsOn(core % "compile;test->test")

lazy val `pool-scala` = (project in file("pool-scala"))
  .settings(
    coreSettings ++ Seq(
      name := "reactive-memcached-pool-scala",
      libraryDependencies ++= Seq(
        "io.github.andrebeat" %% "scala-pool" % "0.4.1"
      )
    )
  )
  .dependsOn(core % "compile;test->test")

lazy val `root` = (project in file("."))
  .settings(coreSettings)
  .settings(
    name := "reactive-memcached-project"
  )
  .aggregate(core, `pool-commons`, `pool-fop`, `pool-scala`, `pool-stormpot`)