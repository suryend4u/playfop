name := """sample-java"""

version := "1.1-SNAPSHOT"

lazy val root = (project in file(".")).
  enablePlugins(PlayJava).
  settings(
    // Avoid creating "doc" information in distribution. (That information
    // included fonts the application would pick up but could not use.)
    sources in (Compile, doc) := Seq.empty
  )

scalaVersion := "2.12.3"

routesGenerator := InjectedRoutesGenerator

libraryDependencies ++= Seq(
  guice,
  "com.dmanchester" %% "playfop" % "1.1-SNAPSHOT",
  "com.adrianhurt" %% "play-bootstrap" % "1.2-P26-B3",
  "org.webjars" % "bootstrap" % "3.3.7-1"
)
