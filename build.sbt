import sbt.Project.projectToRef
import scalapb.compiler.Version.scalapbVersion

inThisBuild(
  Seq(
    organization := "kr.ikhoon.scalapb-reactor",
    homepage := Some(url("https://github.com/ikhoon/scalapb-reactor")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    startYear := Some(2020),
    scalacOptions := Seq(
      "-Xsource:2.13"
    ) ++ Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked")
  )
)

lazy val versions = new {
  val armeria = "1.3.0"
  val collectionCompat = "2.3.1"
  val munit = "0.7.19"
  val reactor = "0.8.0"
  val reactorGrpc = "1.0.1"
  val scala212 = "2.12.12"
  val scala213 = "2.13.4"
}

lazy val root = project
  .in(file("."))
  .settings(
    name := "scalapb-reactor",
    description := "ScalaPB gRPC generator for Project Reactor"
  )
  .aggregate(protocGen.agg)
  .aggregate(`code-gen`, e2e)

// TODO(ikhoon):
//  - Publish 0.1.0 for Scala 2.11, 2.12, 2.13
//  - Publish 0.2.0 for Scala 2.12, 2.13

lazy val `code-gen` = (project in file("code-gen"))
  .settings(publishSettings: _*)
  .settings(
    name := "reactor-grpc-code-gen",
    scalaVersion := versions.scala212,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapbVersion,
      "org.scala-lang.modules" %% "scala-collection-compat" % versions.collectionCompat
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
    crossScalaVersions := Seq(versions.scala212, versions.scala213),
    skip in publish := true,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion,
      "io.projectreactor" %% "reactor-scala-extensions" % versions.reactor,
      "com.salesforce.servicelibs" % "reactor-grpc-stub" % versions.reactorGrpc,
      "com.linecorp.armeria" % "armeria-grpc" % versions.armeria % Test,
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    PB.targets in Compile := Seq(
      scalapb.gen(grpc = true) -> (sourceManaged in Compile).value,
      genModule(
        "scalapb.reactor.ReactorCodeGenerator$"
      ) -> (sourceManaged in Compile).value
    ),
    codeGenClasspath := (`code-gen` / Compile / fullClasspath).value
  )

lazy val publishSettings = List(
  scmInfo := Some(ScmInfo(url("https://github.com/ikhoon/scalapb-reactor"), "git@github.com:http4s/scalapb-reactor.git")),
  developers := List(
    Developer(
      "ikhoon",
      "Ikhun Um",
      "ih.pert@gmail.com",
      url("https://github.com/ikhoon")
    )
  ),
  publishTo := {
    if (isSnapshot.value)
      Some(Opts.resolver.sonatypeSnapshots)
    else
      Some(Opts.resolver.sonatypeStaging)
  },
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  pomIncludeRepository := { _ => false },
  Test / publishArtifact := false,
  credentials ++= (for {
    username <- sys.env.get("SONATYPE_USERNAME")
    password <- sys.env.get("SONATYPE_PASSWORD")
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  ))
)
