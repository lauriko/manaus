import NativePackagerHelper._
import com.typesafe.sbt.packager.docker._

name := "manaus"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val ESClientVersion   = "5.5.0"
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalanlp" %% "breeze" % "0.13",
    "org.scalanlp" %% "breeze-natives" % "0.13",
    "org.elasticsearch" % "elasticsearch" % ESClientVersion,
    "org.elasticsearch.client" % "transport" % ESClientVersion,
    "org.elasticsearch.client" % "rest" % ESClientVersion,
    "org.apache.logging.log4j" % "log4j-api" % "2.7",
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "ch.qos.logback"    %  "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.6.0",
    "com.github.scopt" %% "scopt" % "3.5.0"
  )
}

scalacOptions += "-deprecation"
scalacOptions += "-feature"

enablePlugins(GitVersioning)
enablePlugins(GitBranchPrompt)
enablePlugins(JavaServerAppPackaging)
enablePlugins(UniversalPlugin)
enablePlugins(DockerPlugin)

git.useGitDescribe := true

//http://www.scala-sbt.org/sbt-native-packager/formats/docker.html
dockerCommands := Seq(
  Cmd("FROM", "java:8"),
  Cmd("RUN", "apt", "update"),
  Cmd("RUN", "apt", "install", "-y", "netcat"),
  Cmd("LABEL", "maintainer=\"Angelo Leto <angelo@getjenny.com>\""),
  Cmd("LABEL", "description=\"Docker container for Manaus NLP services\""),
  Cmd("WORKDIR", "/"),
  Cmd("ADD", "/opt/docker", "/manaus"),
  Cmd("VOLUME", "/manaus/data"),
  Cmd("VOLUME", "/manaus/log")
)

packageName in Docker := packageName.value
version in Docker := version.value
dockerRepository := Some("elegansio")

mappings in Universal ++= {
  directory("scripts") ++
  directory("statistics_data") ++
  contentOf("src/main/resources").toMap.mapValues("config/" + _).toSeq
}

// Assembly settings
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}
