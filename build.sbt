organization := "com.github.krasserm"

name := "akka-persistence-testkit"

version := "0.2"

scalaVersion := "2.10.3"

fork := true

parallelExecution in Test := false

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.0" % "compile"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.0" % "compile"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.0" % "compile"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"
