scalaVersion := "2.13.4"

PB.targets in Compile := Seq(
  scalapb.gen() -> (sourceManaged in Compile).value,
  scalapb.reactor.ReactorCodeGenerator -> (sourceManaged in Compile).value
)

libraryDependencies ++= Seq(
  // Use Armeria's gRPC server and client. https://armeria.dev/docs/server-grpc
  "com.linecorp.armeria" % "armeria-grpc" % "1.7.2",
  "com.linecorp.armeria" %% "armeria-scalapb" % "1.3.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalameta" %% "munit" % "0.7.9" % Test
)
