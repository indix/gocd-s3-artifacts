val ant = "org.apache.ant" % "ant" % "1.9.4"
val apacheCommons = "org.apache.commons" % "commons-lang3" % "3.1"
val commonsIo = "commons-io" % "commons-io" % "1.3.2"
val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.10.26"
val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.4.0"
val goPluginLibrary = "cd.go.plugin" % "go-plugin-api" % "14.4.0" % Provided

val junit = "junit" % "junit" % "4.10" % Test
val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test
val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % Test
val mockito = "org.mockito" % "mockito-all" % "1.9.0" % Test
val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0" % Test

val appVersion = sys.env.get("SNAP_PIPELINE_COUNTER") orElse sys.env.get("GO_PIPELINE_LABEL") getOrElse "1.0.0-SNAPSHOT"

lazy val root = project in file(".") aggregate(utils, publish, material, fetch)

lazy val commonSettings = Seq(
  organization := "com.indix",
  version := appVersion,
  scalaVersion := "2.10.4",
  unmanagedBase := file(".") / "lib",
  libraryDependencies ++= Seq(
    apacheCommons, commonsIo, awsS3, goPluginLibrary, mockito
  ),
  variables in EditSource += ("version", appVersion),
  targetDirectory in EditSource <<= baseDirectory(_ / "target" / "transformed"),
  sources in EditSource <++= baseDirectory.map(d => (d / "template" / "plugin.xml").get),
  unmanagedResourceDirectories in Compile += { baseDirectory.value / "target" / "transformed" }
)

lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    name := "utils",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      junit, junitInterface, hamcrest
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
  )

lazy val publish = (project in file("publish")).
  dependsOn(utils % "test->test;compile->compile").
  settings(commonSettings: _*).
  settings(
    name := "s3publish",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      ant, junit, junitInterface, hamcrest
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
  )

lazy val material = (project in file("material")).
  dependsOn(utils).
  settings(commonSettings: _*).
  settings(
    name := "s3material",
    crossPaths := false,
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    libraryDependencies ++= Seq(
      scalaTest
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
  )

lazy val fetch = (project in file("fetch")).
  dependsOn(utils % "test->test;compile->compile").
  settings(commonSettings: _*).
  settings(
    name := "s3fetch",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      junit, hamcrest
    ),
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7")
  )
