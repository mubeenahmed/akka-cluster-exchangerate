name := "IntelliJ"

version := "0.1"

scalaVersion := "2.13.8"


val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"
val AkkaManagementVersion = "1.1.3"


Compile / scalacOptions ++= Seq(
  "-target:11",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-Xlog-reflective-calls",
  "-Xlint")
Compile / javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

Test / parallelExecution := false
Test / testOptions += Tests.Argument("-oDF")
Test / logBuffered := false

run / fork := true
Global / cancelable := false // ctrl-c

libraryDependencies ++= Seq(

  "com.typesafe.akka" %% "akka-slf4j" % AkkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.10",

  "com.lightbend.akka" %% "akka-stream-alpakka-elasticsearch" % "3.0.4",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,

  "com.typesafe.akka" %% "akka-cluster-typed" % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % AkkaVersion,

  "com.lightbend.akka.management" %% "akka-management" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % AkkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % AkkaManagementVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % AkkaVersion,

  "com.typesafe.akka" %% "akka-persistence-typed" % AkkaVersion,

  "com.typesafe.akka" %% "akka-discovery" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  "com.typesafe.akka" %% "akka-persistence" % AkkaVersion,
  "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.4",
  "mysql" % "mysql-connector-java" % "8.0.28",

  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)