// ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.6")
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.8")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.11"


// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")

// Scalac options
addSbtPlugin("io.github.davidgregory084" % "sbt-tpolecat" % "0.1.22")

// Release
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.11.0")
addSbtPlugin("com.github.sbt" % "sbt-ci-release" % "1.5.10")

// Header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.8.0")
