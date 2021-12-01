
lazy val memoryLimit = settingKey[String]("JVM memory limit")

lazy val akkaVersion = "2.6.17"
lazy val akkaHttpVersion = "10.2.7"
lazy val akkaGrpcVersion = "2.1.1"
lazy val akkaManagementVersion = "1.1.1"
lazy val akkaProjectionVersion = "1.2.2"

lazy val commonSettings = Seq(
  version := "0.1",
  scalaVersion := "2.13.7",

  organizationName := "martyn82",
  organizationHomepage := Some(url("https://github.com/martyn82")),
  organization := "com.github.martyn82",
  startYear := Some(2021),

  updateOptions := updateOptions.value
    .withGigahorse(false)
    .withLatestSnapshots(true),

  memoryLimit := "384m",

  resolvers ++= Seq(
    DefaultMavenRepository
  ),

  scalacOptions ++= Seq(
    "-feature",
    "-deprecation",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-Xlint",
    "-Ywarn-dead-code",
    "-Ywarn-numeric-widen"
  ),

  javaOptions ++= Seq(
    "-Xmx" + memoryLimit.value
  ),

  run / fork := true,
  Test / fork := true,
  Test / parallelExecution := true,
  Test / publishArtifact := false,
  Test / logBuffered := false,
  Global / cancelable := true
)

lazy val `greeter-api` = project
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    commonSettings,

    name := "greeter-api",

    akkaGrpcCodeGeneratorSettings += "flat_package",

    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,

      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaVersion,
    )
  )

lazy val `greeter-server` = project
  .dependsOn(
    `greeter-api`
  )
  .settings(
    commonSettings,

    name := "greeter-server",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaVersion,

      "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
      "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

      "com.lightbend.akka" %% "akka-projection-cassandra" % akkaProjectionVersion,
      "com.lightbend.akka" %% "akka-projection-eventsourced" % akkaProjectionVersion,

      "org.slf4j" % "slf4j-simple" % "1.7.32",
      "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

      "com.sksamuel.pulsar4s" %% "pulsar4s-core" % "2.7.3",
    ),

    dependencyOverrides ++= Seq(
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.0",
    )
  )

lazy val `greeter-client` = project
  .dependsOn(
    `greeter-api`
  )
  .settings(
    commonSettings,

    name := "greeter-client",

    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http2-support" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery" % akkaVersion,
      "com.typesafe.akka" %% "akka-pki" % akkaVersion,
    )
  )
