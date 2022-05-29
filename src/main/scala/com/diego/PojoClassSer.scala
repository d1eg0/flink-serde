package com.diego

import com.diego.models._
import org.apache.flink.api.common.functions.MapFunction
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.extensions._
import org.slf4j.LoggerFactory

import collection.JavaConverters._
import scala.util.Try

object PojoClassSer extends App {

  val NUM_EVENTS: Int = Try(args(0).toInt).getOrElse(1000000)

  def processPojoClass(env: StreamExecutionEnvironment) = {
    val elements = 1 to NUM_EVENTS map (i => new PointPojo(i, i * 2, i * 3))
    val ds       = env.fromCollection(elements.asJava)

    ds.map(new MapFunction[PointPojo, PointPojo] {
      override def map(value: PointPojo): PointPojo =
        value
    })
    ds.addSink(new PrintSinkFunction[PointPojo])
    env.execute("pojo class")
  }

  val logger = LoggerFactory.getLogger(this.getClass())
  logger.info("Pojo class serialization")

  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig().disableGenericTypes()

  processPojoClass(env)

}
