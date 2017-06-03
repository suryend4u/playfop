// In a typical sbt project, task "publishLocal" publishes locally, and
// task "publish" does so remotely. In this one, both do so locally.
// "publishLocal" is intended primarily for snapshot builds of just the
// PlayFOP JAR. "publish" is intended for release builds of that JAR, and of
// other artifacts.

lazy val publishDir = settingKey[File]("The directory to which 'publish...' tasks (except 'publishLocal') publish artifacts.")

lazy val cleanAll = taskKey[Unit]("Cleans all projects and deletes publishDir.")

lazy val publishAll = taskKey[Unit]("Publishes all six artifacts to publishDir.")

lazy val publishAndFlatten = taskKey[Unit]("""Builds and publishes to publishDir the PlayFOP JAR
                                             |and POM, as well as a JAR of sources. The directory
                                             |structure that would normally accompany publication is
                                             |flattened such that all files are placed at the top
                                             |level of publishDir.""".stripMargin.replaceAll("\n", " "))

lazy val publishDoc = taskKey[Unit]("""To publishDir, publishes Scaladoc, Javadoc, or the user guide
                                      |(depending on the project).""".stripMargin.replaceAll("\n", " "))

lazy val root = (project in file(".")).
  settings(commonSettings: _*).
  settings(
      cleanAll := {
        clean.value
        (clean in scaladocOnly).value
        (clean in javadocOnly).value
        (clean in userguide).value
        IO.delete(publishDir.value)
      },
      publishAll := {
        publishAndFlatten.value
        (publishDoc in scaladocOnly).value
        (publishDoc in javadocOnly).value
        (publishDoc in userguide).value
      },
      publishTo := Some(Resolver.file("file", publishDir.value)),
      publishArtifact in (Compile, packageDoc) := false,  // don't publish Javadocs from this project; look to "javadocOnly" project for that
      publishAndFlatten := {
        publish.value
        val organizationPart1 = organization.value.split("\\.").head  // extracts "com" from "com.dmanchester"
        val jarAndPomFinder = publishDir.value / organizationPart1 ** ("*.jar" || "*.pom")
        val jarsAndPom = jarAndPomFinder.get

        val log = streams.value.log
        log.info("Flattening " + publishDir.value + "...")
        IO.move(jarsAndPom.map { srcFile =>
            srcFile -> (publishDir.value / srcFile.getName())
        })
        IO.delete(publishDir.value / organizationPart1)
      }
  )

lazy val scaladocOnly = project.
  settings(commonSettings: _*).
  settings(
    scalaSource in Compile := file("./src/main/scala"),
    target := file("./target-scaladoc"),
    publishDoc := {
      val docJarSource = (packageDoc in Compile).value
      val docJarTargetName = docJarSource.getName().replace("javadoc", "scaladoc")  // address anomaly in JAR name: even though it contains Scaladoc, name suggests Javadoc
      val docJarTarget = new File(publishDir.value, docJarTargetName)

      val log = streams.value.log
      log.info("Copying " + docJarSource + " to " + docJarTarget + "...")
      IO.copyFile(docJarSource, docJarTarget)
    }
  )

lazy val javadocOnly = project.
  settings(commonSettings: _*).
  settings(
    javaSource in Compile := file("./src/main/java"),
    target := file("./target-javadoc"),
    publishDoc := {
      val docJarSource = (packageDoc in Compile).value
      val docJarTarget = new File(publishDir.value, docJarSource.getName())

      val log = streams.value.log
      log.info("Copying " + docJarSource + " to " + docJarTarget + "...")
      IO.copyFile(docJarSource, docJarTarget)
    }
  )

lazy val userguide = scalatex.ScalatexReadme(
  projectId = "userguide",
  wd = file(""),
  url = "https://github.com/lihaoyi/scalatex/tree/master",  // TODO Should point to Scalatex project, or our own?
  source = "UserGuide"
).settings(commonSettings: _*).  // only common setting we need is publishDir
  settings(
    publishDoc := {
      (run in Compile).toTask("").value
      val userguideDir = target.value / "scalatex"
      val zipFile = name.value + "-" + version.value + "-userguide.zip"
      val zipFileWithPath = publishDir.value / zipFile

      val log = streams.value.log
      log.info("Zipping " + userguideDir + " to " + zipFileWithPath + "...")
      val fileFinder = userguideDir ** "*.*"
      val filesToZip = fileFinder.get

      IO.zip(filesToZip.map { srcFile =>
        val srcFileWithoutUserguideDir = srcFile.getPath().substring(userguideDir.getPath().length())  // represent directories in zip relative to userguideDir
        srcFile -> srcFileWithoutUserguideDir
      }, zipFileWithPath)
    }
  )

lazy val commonSettings = Seq(
  name := "playfop",
  organization := "com.dmanchester",
  version := "0.4-SNAPSHOT",
  scalaVersion := "2.11.7",
  publishDir := new File("./dist"),
  libraryDependencies ++= Seq(
    "com.typesafe.play" %% "play" % "2.3.0" % "provided",
    "org.apache.xmlgraphics" % "fop" % "2.1",
    "org.apache.pdfbox" % "pdfbox" % "1.8.9" % "test",
    // The following PlayFOP dependencies are shared with other libraries
    // specified in this file. We allow those libraries' use of them to
    // determine the version numbers below. Consult
    // ".../target/resolution-cache/reports" for more information.
    "com.typesafe.play" %% "twirl-api" % "1.0.0" % "provided",  // see also: https://github.com/playframework/playframework/blob/2.3.0/framework/project/Dependencies.scala
    "org.slf4j" % "slf4j-api" % "1.7.6",
    "commons-io" % "commons-io" % "1.3.1",                      // pretty old (2007?), but sufficient for our needs
    "org.scala-lang.modules" %% "scala-xml" % "1.0.5",
    "org.specs2" %% "specs2-core" % "2.3.12" % "test",
    "junit" % "junit" % "4.11" % "test"
    // ...end shared dependencies.
  ),
  resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",  // per https://etorreborre.github.io/specs2/website/SPECS2-3.6/quickstart.html
  scalacOptions ++= Seq("-deprecation", "-feature"),  // per http://alvinalexander.com/scala/scala-sbt-re-run-with-deprecation-feature-message
  scalacOptions in Test ++= Seq("-Yrangepos")  // also per specs2 Quick Start
)