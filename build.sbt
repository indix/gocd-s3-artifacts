val ant = "org.apache.ant" % "ant" % "1.9.4"
val apacheCommons = "org.apache.commons" % "commons-lang3" % "3.1"
val commonsIo = "commons-io" % "commons-io" % "1.3.2"
val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.9.11"
val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "1.6.0"
val concurrentLinkedHashmap = "com.googlecode.concurrentlinkedhashmap" % "concurrentlinkedhashmap-lru" % "1.4.1"

val junit = "junit" % "junit" % "4.10" % Test
val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test
val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % Test
val mockito = "org.mockito" % "mockito-all" % "1.9.0" % Test
val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0" % Test

lazy val root = project in file(".") aggregate(utils, publish, material, fetch)

lazy val commonSettings = Seq(
  organization := "com.indix",
  version := "0.1.0",
  scalaVersion := "2.10.3",
  unmanagedBase := file(".") / "lib"
)

lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    name := "utils",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      apacheCommons, commonsIo, awsS3, junit, junitInterface, hamcrest, mockito
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6")
  )

lazy val publish = (project in file("publish")).
  dependsOn(utils).
  settings(commonSettings: _*).
  settings(
    name := "s3publish",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      ant, apacheCommons, commonsIo, awsS3, junit, junitInterface, hamcrest, mockito
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6")
  )

lazy val material = (project in file("material")).
  settings(commonSettings: _*).
  settings(
    name := "s3material",
    crossPaths := false,
    libraryDependencies ++= Seq(
      apacheCommons, commonsIo, awsS3, nscalaTime, concurrentLinkedHashmap, scalaTest, mockito
    )
  )

lazy val fetch = (project in file("fetch")).
  dependsOn(utils).
  settings(commonSettings: _*).
  settings(
    name := "s3fetch",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      apacheCommons, commonsIo, awsS3, junit, hamcrest, mockito
    ),
    javacOptions ++= Seq("-source", "1.6", "-target", "1.6")
  )
