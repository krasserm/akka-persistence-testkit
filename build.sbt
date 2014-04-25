organization := "com.github.krasserm"

name := "akka-persistence-testkit"

version := "0.3.1"

scalaVersion := "2.11.0"

crossScalaVersions := Seq("2.10.4", "2.11.0")

fork := true

parallelExecution in Test := false

libraryDependencies += "com.typesafe.akka" %% "akka-persistence-experimental" % "2.3.2" % "compile"

libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.3.2" % "compile"

libraryDependencies += "org.scalatest" %% "scalatest" % "2.1.3" % "compile"

libraryDependencies += "commons-io" % "commons-io" % "2.4" % "test"

libraryDependencies := {
  CrossVersion.partialVersion(scalaVersion.value) match {
    case Some((2, scalaMajor)) if scalaMajor >= 11 =>
      libraryDependencies.value ++ Seq("org.scala-lang.modules" %% "scala-xml" % "1.0.1" % "test")
    case _ =>
      libraryDependencies.value
  }
}
