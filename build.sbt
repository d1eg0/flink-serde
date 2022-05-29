import Dependencies._

ThisBuild / scalaVersion := "2.12.13"
ThisBuild / organization := "com.diego"

resolvers += "Confluent" at "https://packages.confluent.io/maven/"

lazy val flinkSerialization = (project in file("."))
  .settings(
    name := "flink-serialization",
    libraryDependencies ++= Dependencies.all,
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue,
    Test / sourceGenerators += (Test / avroScalaGenerateSpecific).taskValue
  )
