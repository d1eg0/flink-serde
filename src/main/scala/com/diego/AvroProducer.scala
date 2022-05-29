package com.diego

import com.diego.models.Point
import com.diego.models.PointAvro
import com.diego.serde.PointSerializer
import com.diego.serde.Serde._
import com.sksamuel.avro4s.AvroOutputStream
import com.sksamuel.avro4s.AvroSchema
import com.sksamuel.avro4s.SchemaFor
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.Schema
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.serialization.SerializationSchema.InitializationContext
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.flink.connector.kafka.sink.KafkaSink
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory

import java.io.ByteArrayOutputStream
import java.lang

import collection.JavaConverters._
import scala.util.Try

object AvroProducer extends App {
  val NUM_EVENTS: Int = Try(args(0).toInt).getOrElse(100)
  
  val logger          = LoggerFactory.getLogger(this.getClass())
  logger.info("Avro class serialization")

  val topic             = "points_events"
  val schemaRegistryURL = "http://localhost:8081"

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig().disableGenericTypes()
  env.getConfig().enableForceAvro()

  val elements = 1 to NUM_EVENTS map (i => PointAvro(i, Some(i * 2), Some(i * 3)))
  val ds       = env.fromCollection(elements.asJava)

  ds.map(new MapFunction[PointAvro, PointAvro] {
    override def map(value: PointAvro): PointAvro =
      value
  })

  val kafkaSink = KafkaSink
    .builder()
    .setBootstrapServers("localhost:29092")
    .setRecordSerializer(new PointSerializer(topic, schemaRegistryURL))
    .build()

  ds.sinkTo(kafkaSink)
  env.execute("avro serialization")
}
