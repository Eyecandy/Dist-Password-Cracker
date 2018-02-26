

lazy val root = (project in file(".")).
  settings(
    name := "CrackerSystem",
    version := "0.1",
    scalaVersion := "2.12.4",
    mainClass in Compile := Some("Main")
  )

libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.11"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.9"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.9"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.0-RC1"




