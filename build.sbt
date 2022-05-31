import Dependencies._

ThisBuild / scalaVersion := "2.12.13"
ThisBuild / organization := "com.diego"

resolvers += "Confluent" at "https://packages.confluent.io/maven/"

lazy val flinkSerialization = (project in file("."))
  .settings(
    name := "flink-serialization",
    libraryDependencies ++= Dependencies.all,
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue,
    Test / sourceGenerators += (Test / avroScalaGenerateSpecific).taskValue,
    assembly / mainClass                    := Some("com.diego.AvroProducer"),
    assemblyPackageScala / assembleArtifact := false,
    assembly / assemblyJarName              := "flink-serialization.jar",
    assembly / assemblyMergeStrategy := {
      case PathList("javax", "servlet", xs @ _*) => MergeStrategy.first
      case PathList("META-INF", xs @ _*)         => MergeStrategy.discard
      case "application.conf"                    => MergeStrategy.concat
      case x                                     => MergeStrategy.first
    }
  )
