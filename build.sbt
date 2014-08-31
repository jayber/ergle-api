import com.typesafe.sbt.SbtNativePackager.packageArchetype

name := "ergle-api"

version := "1.0-SNAPSHOT"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    "org.springframework" % "spring-context" % "3.2.2.RELEASE",
    "javax.inject" % "javax.inject" % "1",
    "org.mockito" % "mockito-core" % "1.9.5",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.11.0-SNAPSHOT",
    "org.specs2" %% "specs2" % "2.3.7" % "test",
    "org.apache.james" % "apache-mime4j" % "0.7.2"
)

libraryDependencies += ws

lazy val root = (project in file(".")).enablePlugins(PlayScala)

packageArchetype.java_application

