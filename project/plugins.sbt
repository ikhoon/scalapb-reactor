// ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.4")
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.7")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.3"

// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

// Scalac options
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.20")

// Release
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")

// Header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")
