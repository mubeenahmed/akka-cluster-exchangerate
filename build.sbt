name := "IntelliJ"

version := "0.1"

scalaVersion := "2.13.8"


val AkkaVersion = "2.6.18"
val AkkaHttpVersion = "10.2.7"
val AkkaManagementVersion = "1.1.3"

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
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "com.typesafe.akka" %% "akka-persistence-testkit" % AkkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.10" % Test
)