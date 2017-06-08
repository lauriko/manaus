import NativePackagerHelper._

name := "manaus"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  val ESClientVersion   = "5.3.2"
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalanlp" %% "breeze" % "0.13",
    "org.scalanlp" %% "breeze-natives" % "0.13",
    "org.elasticsearch" % "elasticsearch" % ESClientVersion,
    "org.elasticsearch.client" % "transport" % ESClientVersion,
    "org.elasticsearch.client" % "rest" % ESClientVersion,
    "org.apache.logging.log4j" % "log4j-api" % "2.7",
    "org.apache.logging.log4j" % "log4j-core" % "2.7",
    "ch.qos.logback"    %  "logback-classic" % "1.1.3",
    "com.github.scopt" %% "scopt" % "3.5.0"
  )
}

scalacOptions += "-deprecation"
scalacOptions += "-feature"

enablePlugins(JavaServerAppPackaging)
enablePlugins(GitVersioning)

// Assembly settings
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

