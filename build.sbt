name := "ergle-api"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  cache,
    "org.springframework" % "spring-context" % "3.2.2.RELEASE",
    "javax.inject" % "javax.inject" % "1",
    "org.mockito" % "mockito-core" % "1.9.5",
    "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
    "org.specs2" %% "specs2" % "2.3.7" % "test"
)

play.Project.playScalaSettings

packageArchetype.java_application

