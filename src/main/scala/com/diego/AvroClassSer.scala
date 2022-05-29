package com.diego

import com.diego.models.PointAvro
import com.sksamuel.avro4s.AvroSchema
import com.sksamuel.avro4s.SchemaFor
import org.apache.avro.Schema
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import scala.util.Try

object AvroClassSer extends App {
  val NUM_EVENTS: Int = Try(args(0).toInt).getOrElse(100)

  val logger = LoggerFactory.getLogger(this.getClass())
  logger.info("Avro class serialization")

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig().disableGenericTypes()
  env.getConfig().enableForceAvro()

  val elements = 1 to NUM_EVENTS map (i => PointAvro(i, Some(i * 2), Some(i * 3)))
  val ds       = env.fromCollection(elements.asJava)

  ds.map(new MapFunction[PointAvro, PointAvro] {
    override def map(value: PointAvro): PointAvro =
      value
  })
  ds.addSink(new PrintSinkFunction[PointAvro])
  env.execute("avro serialization")
}
