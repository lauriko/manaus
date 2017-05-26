import NativePackagerHelper._

name := "manaus"

version := "0.1"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.bintrayRepo("hseeberger", "maven"))

//https://mvnrepository.com/artifact/org.elasticsearch

libraryDependencies ++= {
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "com.github.scopt" %% "scopt" % "3.5.0"
  )
}

enablePlugins(JavaServerAppPackaging)

// Assembly settings
mainClass in Compile := Some("com.getjenny.manaus.commands.SampleKeywordExtraction")
mainClass in assembly := Some("com.getjenny.manaus.commands.SampleKeywordExtraction")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

