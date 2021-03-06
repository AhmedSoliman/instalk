name := "instalk"

organization := "im.instalk"

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.2"

val akkaVersion = "2.3.5"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8", "-feature")

libraryDependencies ++= Seq(
  //"com.typesafe.akka"	%%	"akka-actor"					%	akkaVersion,
  "com.typesafe.akka"       %%	"akka-cluster"					%	akkaVersion,
  "com.typesafe.akka"       %%	"akka-contrib"					%	akkaVersion,
  "com.typesafe.akka"       %%	"akka-testkit"					%	akkaVersion,
  "com.typesafe.akka"       %%  "akka-slf4j"					  %	akkaVersion   %   "test",
  "org.scalatest"           %%  "scalatest" 				    % "2.2.1"      	%   "test",
  "com.datastax.cassandra"  % "cassandra-driver-core"   % "2.0.4",
  "ch.qos.logback"          %   "logback-classic"				% "1.1.2",
  "com.codahale.metrics"    % "metrics-core" % "3.0.2"
)

incOptions := incOptions.value.withNameHashing(true) 

lazy val root = (project in file(".")).enablePlugins(PlayScala)
