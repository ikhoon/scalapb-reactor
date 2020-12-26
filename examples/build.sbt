scalaVersion := "2.11.12"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-target:jvm-1.8"
)

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value,
  scalapb.reactor.ReactorCodeGenerator -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  // Use Armeria's gRPC server and client. https://armeria.dev/docs/server-grpc
  "com.linecorp.armeria" % "armeria-grpc" % "1.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalameta" %% "munit" % "0.7.9" % Test
)
