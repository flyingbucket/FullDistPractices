ThisBuild / scalaVersion     := "2.13.16"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val sparkVersion = "4.0.1"

lazy val root = (project in file("."))
  .settings(
    name := "FullDistPractices",
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
      "org.apache.spark" %% "spark-sql"  % sparkVersion % "provided",
      "org.scalanlp" %% "breeze" % "2.1.0",
      "org.scalanlp" %% "breeze-natives" % "2.1.0"
    ),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", _ @ _*) => MergeStrategy.discard
      case _                            => MergeStrategy.first
    }
  )
