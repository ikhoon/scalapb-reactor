import scalapb.compiler.Version.scalapbVersion

val Scala213 = "2.13.3"

val Scala212 = "2.12.12"

inThisBuild(
  Seq(
    organization := "com.github.ikhoon",
    name := "scalapb-reactor",
    homepage := Some(url("https://github.com/ikhoon/scalapb-reactor")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    startYear := Some(2020),
    scalacOptions := Seq(
      "-Xsource:2.13"
    ) ++ Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked")

  )
)

lazy val `code-gen` = (project in file("code-gen"))
  .settings(
    name := "scalapb-reactor-code-gen",
    scalaVersion := Scala212,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin"       % scalapbVersion,
      "org.scala-lang.modules" %% "scala-collection-compat" % "2.3.1"
    )
  )

lazy val protocGen = protocGenProject("protoc-gen-scalapb-reactor", `code-gen`)
  .settings(
    Compile / mainClass := Some("scalapb.reactor.ReactorCodeGenerator")
  )

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    crossScalaVersions := Seq(Scala212, Scala213),
    skip in publish := true,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapb.compiler.Version.scalapbVersion,
      "io.projectreactor" %% "reactor-scala-extensions" % "0.8.0",
      "com.salesforce.servicelibs" % "reactor-grpc-stub" % "1.0.1",
      "com.linecorp.armeria" % "armeria-grpc" % "1.3.0"
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      genModule(
        "scalapb.reactor.ReactorCodeGenerator$"
      )                        -> (sourceManaged in Compile).value
    ),
    codeGenClasspath := (`code-gen` / Compile / fullClasspath).value
  )
