This / organization := "com.akkaEssentials"

This / scalaVersion := "2.12.17"

val akkaVersion = "2.8.0"

val scalaTestVersion = "3.2.15"

val logbackV = "1.4.7"

val scalaLoggingV = "3.9.5"


libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "org.scalatest"     %% "scalatest" % scalaTestVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingV,
  "ch.qos.logback" % "logback-classic" % logbackV,

)