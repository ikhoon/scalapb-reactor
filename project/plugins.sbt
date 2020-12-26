// ScalaPB
addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC4")
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.5")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.9.8"

// Scalafmt
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.2")

// Release
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.13")
addSbtPlugin("com.jsuereth" % "sbt-pgp" % "2.0.1")

// Header
addSbtPlugin("de.heikoseeberger" % "sbt-header" % "5.6.0")
