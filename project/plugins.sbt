// ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.3")
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.7")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.11"

// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

// Release
addSbtPlugin("com.eed3si9n" % "sbt-buildinfo" % "0.10.0")
addSbtPlugin("com.geirsson" % "sbt-ci-release" % "1.5.7")

// Header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")
