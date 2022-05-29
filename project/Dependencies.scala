import sbt._

object Dependencies {

  private object flink {
    lazy val version   = "1.14.4"
    lazy val namespace = "org.apache.flink"

    lazy val core      = namespace  % "flink-core"            % version
    lazy val streaming = namespace %% "flink-streaming-scala" % version
    lazy val avro      = namespace  % "flink-avro"            % version
    lazy val clients   = namespace %% "flink-clients"         % version
    lazy val kafka     = namespace %% "flink-connector-kafka" % version
  }

  private object kafkaAvroSerde {
    lazy val version   = "7.1.1"
    lazy val namespace = "io.confluent"
    lazy val core      = namespace % "kafka-avro-serializer" % version
  }

  lazy val flinkDependencies =
    Seq(flink.core, flink.streaming, flink.avro, flink.clients, flink.kafka).map(_.exclude("org.slf4j", "*"))

  // Serialization
  val avro4s = "com.sksamuel.avro4s" %% "avro4s-core" % "4.0.13" % "compile"

  val logback = "ch.qos.logback" % "logback-classic" % "1.2.11"

  val all = flinkDependencies ++ Seq(avro4s, logback, kafkaAvroSerde.core)
}
