val ant = "org.apache.ant" % "ant" % "1.9.4"
val apacheCommons = "org.apache.commons" % "commons-lang3" % "3.1"
val commonsIo = "commons-io" % "commons-io" % "1.3.2"
val awsS3 = "com.amazonaws" % "aws-java-sdk-s3" % "1.11.127"
val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.4.0"
val gson = "com.google.code.gson" % "gson" % "2.2.3"
val goPluginLibrary = "cd.go.plugin" % "go-plugin-api" % "23.4.0"
val jaxbApi = "javax.xml.bind" % "jaxb-api" % "2.3.0"
val servlet = "javax.servlet" % "javax.servlet-api" % "3.1.0" 

val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.3" % Test
val junit = "junit" % "junit" % "4.12" % Test
val junitInterface = "com.novocode" % "junit-interface" % "0.11" % Test
val mockito = "org.mockito" % "mockito-all" % "1.10.19" % Test
val scalaTest = "org.scalatest" %% "scalatest" % "2.2.0" % Test

val appVersion = sys.env.get("TRAVIS_TAG") orElse sys.env.get("BUILD_LABEL") getOrElse s"1.0.0-${System.currentTimeMillis / 1000}-SNAPSHOT"


lazy val root = Project(
  id = "gocd-s3-artifacts",
  base = file(".")
) aggregate(utils, publish, material, fetch)

lazy val commonSettings = Seq(
  organization := "com.indix",
  version := appVersion,
  scalaVersion := "2.11.12",
  unmanagedBase := file(".") / "lib",
  libraryDependencies ++= Seq(
    apacheCommons, commonsIo, awsS3, goPluginLibrary, gson, servlet, jaxbApi
  ),
  resourceGenerators in Compile += Def.task {
    val inputFile = baseDirectory.value / "template" / "plugin.xml"
    val outputFile = (resourceManaged in Compile).value / "plugin.xml"
    val contents = IO.read(inputFile)
    IO.write(outputFile, contents.replaceAll("\\$\\{version\\}", appVersion))
    Seq(outputFile)
  }.taskValue,
  mappings in (Compile, packageBin) += {
   (resourceManaged in Compile).value / "plugin.xml" -> "plugin.xml"
  },
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  assemblyMergeStrategy in assembly := {
    case "module-info.class" => MergeStrategy.discard
    case x =>
      val oldStrategy = (assemblyMergeStrategy in assembly).value
      oldStrategy(x)
  }
)

lazy val utils = (project in file("utils")).
  settings(commonSettings: _*).
  settings(
    name := "utils",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      junit, junitInterface, mockito
    ),
    resourceGenerators in Compile := Seq()
  )

lazy val publish = (project in file("publish")).
  dependsOn(utils % "test->test;compile->compile").
  settings(commonSettings: _*).
  settings(
    name := "s3publish",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      ant, junit, junitInterface, mockito
    )
  )

lazy val material = (project in file("material")).
  dependsOn(utils).
  settings(commonSettings: _*).
  settings(
    name := "s3material",
    crossPaths := false,
    assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false),
    libraryDependencies ++= Seq(
      scalaTest, mockito
    )
  )

lazy val fetch = (project in file("fetch")).
  dependsOn(utils % "test->test;compile->compile").
  settings(commonSettings: _*).
  settings(
    name := "s3fetch",
    crossPaths := false,
    autoScalaLibrary := false,
    libraryDependencies ++= Seq(
      junit, mockito
    )
  )
