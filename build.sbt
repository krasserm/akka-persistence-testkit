organization := "com.github.krasserm"

name := "akka-persistence-testkit"

version := "0.3.4"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.10.4", "2.11.0")

fork := true

parallelExecution in Test := false

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.4" % "compile"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.4" % "compile"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.4" % "compile"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"
