name := "instalk"

organization := "im.instalk"

version := "1.0-SNAPSHOT"

//scalaVersion := "2.10.4"

val akkaVersion = "2.3.1"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies ++= Seq(
  //"com.typesafe.akka"	%%	"akka-actor"					%	akkaVersion,
  "com.typesafe.akka"	%%	"akka-cluster"					%	akkaVersion,
  "com.typesafe.akka"	%%	"akka-persistence-experimental" %	akkaVersion,
  "com.typesafe.akka"	%%	"akka-testkit"					%	akkaVersion,
  "com.typesafe.akka"   %%  "akka-slf4j"					%	akkaVersion       %   "test",
  "org.scalatest"       %%  "scalatest" 				    %   "2.1.3"      	%   "test",
  "ch.qos.logback"      %   "logback-classic"				%   "1.1.2"
  //"redis.clients"		    %   "jedis"   							%   "2.4.2"
)     

lazy val root = (project in file(".")).addPlugins(PlayScala)
