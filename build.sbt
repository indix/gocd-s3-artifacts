val apacheCommons = "org.apache.commons" % "commons-lang3" % "3.1"
val commonsIo = "commons-io" % "commons-io" % "1.3.2"
val aws = "com.amazonaws" % "aws-java-sdk-s3" % "1.9.11"
val junit = "junit" % "junit" % "4.10" % Test
val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % Test
val mockito = "org.mockito" % "mockito-all" % "1.9.0" % Test


lazy val root = project in file(".") aggregate(publish)

lazy val commonSettings = Seq(
  organization := "com.indix",
  version := "0.1.0",
  scalaVersion := "2.10.3",
  unmanagedBase := file(".") / "lib"
)

lazy val publish = (project in file("publish")).
  settings(commonSettings: _*).
  settings(
    name := "s3publish",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      apacheCommons, commonsIo, aws, junit, hamcrest, mockito
    )
  )
