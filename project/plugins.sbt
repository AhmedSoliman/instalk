// Comment to get more information during initialization
logLevel := Level.Warn

// The Typesafe repository 
resolvers ++= Seq(
  "Sonatype OSS Releases"  at "http://oss.sonatype.org/content/repositories/releases/",
  //"Sonatype OSS Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
  //"Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots/"
)

// Use the Play sbt plugin for Play projects
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.3-SNAPSHOT")
