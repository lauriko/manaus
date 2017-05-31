import NativePackagerHelper._

name := "manaus"

version := "0.1"

scalaVersion := "2.12.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.bintrayRepo("hseeberger", "maven"))

libraryDependencies ++= {
  Seq(
    "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    "org.scalanlp" %% "breeze" % "0.13",
    "org.scalanlp" %% "breeze-natives" % "0.13",
    "com.github.scopt" %% "scopt" % "3.5.0"
  )
}

scalacOptions += "-deprecation"
scalacOptions += "-feature"

enablePlugins(JavaServerAppPackaging)
enablePlugins(GitVersioning)

// Assembly settings
mainClass in Compile := Some("com.getjenny.manaus.commands.KeywordExtraction")
mainClass in assembly := Some("com.getjenny.manaus.commands.KeywordExtraction")

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

