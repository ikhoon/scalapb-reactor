addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.0-RC4")

// ScalaPB generator
addSbtPlugin("com.thesamet" % "sbt-protoc-gen-project" % "0.1.5")
libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.10.9"
