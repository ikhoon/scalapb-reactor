import sbtprotoc.ProtocPlugin.autoImport.PB
import scalapb.compiler.Version.scalapbVersion

inThisBuild(
  Seq(
    scalaVersion := versions.scala212,
    crossScalaVersions := List(versions.scala213, versions.scala212),
    organization := "kr.ikhoon.scalapb-reactor",
    homepage := Some(url("https://github.com/ikhoon/scalapb-reactor")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "ikhoon",
        "Ikhun Um",
        "ih.pert@gmail.com",
        url("https://github.com/ikhoon")
      )
    ),
    startYear := Some(2020),
    scalacOptions := Seq(
      "-Xsource:2.13"
    ) ++ Seq("-deprecation", "-encoding", "UTF-8", "-feature", "-unchecked")
  )
)

lazy val versions = new {
  val armeria = "1.3.0"
  val collectionCompat = "2.4.4"
  val munit = "0.7.26"
  val reactor = "0.8.0"
  val reactorGrpc = "1.0.1"
  val scala212 = "2.12.13"
  val scala213 = "2.13.5"
}

lazy val root = project
  .in(file("."))
  .settings(
    sonatypeProfileName := "kr.ikhoon",
    publish / skip := true,
    name := "scalapb-reactor",
    description := "ScalaPB gRPC generator for Project Reactor"
  )
  .aggregate(protocGen.agg)
  .aggregate(codeGen, e2e)

lazy val codeGen = (project in file("code-gen"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    name := "scalapb-reactor-codegen",
    scalaVersion := versions.scala212,
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "scalapb.reactor",
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "compilerplugin" % scalapbVersion,
      "org.scala-lang.modules" %% "scala-collection-compat" % versions.collectionCompat
    )
  )

lazy val protocGen = protocGenProject("protoc-gen-scalapb-reactor", codeGen)
  .settings(
    Compile / mainClass := Some("scalapb.reactor.ReactorCodeGenerator"),
    publishTo := sonatypePublishToBundle.value,
    scalaVersion := versions.scala212
  )

lazy val e2e = project
  .in(file("e2e"))
  .enablePlugins(LocalCodeGenPlugin)
  .settings(
    crossScalaVersions := Seq(versions.scala212, versions.scala213),
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalapbVersion,
      "io.projectreactor" %% "reactor-scala-extensions" % versions.reactor,
      "com.salesforce.servicelibs" % "reactor-grpc-stub" % versions.reactorGrpc,
      "com.linecorp.armeria" % "armeria-grpc" % versions.armeria % Test,
      "org.scalameta" %% "munit" % versions.munit % Test
    ),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value,
      genModule(
        "scalapb.reactor.ReactorCodeGenerator$"
      ) -> (Compile / sourceManaged).value
    ),
    codeGenClasspath := (codeGen / Compile / fullClasspath).value
  )
