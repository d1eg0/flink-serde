package com.diego

import com.sksamuel.avro4s.AvroSchema
import com.diego.models.Point
import org.slf4j.LoggerFactory

object ShowAvroSchema extends App {
  val logger = LoggerFactory.getLogger(this.getClass())

  val pointAvroSchema = AvroSchema[Point]

  logger.info(
    s"Point schema:\n$pointAvroSchema\nFor schema registry:\n${pointAvroSchema.toString().replace(""""""", """\"""")}"
  )
}
