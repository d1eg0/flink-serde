package com.diego.serde

import com.diego.models.PointAvro
import com.sksamuel.avro4s.AvroOutputStream
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema
import org.apache.kafka.clients.producer.ProducerRecord

import java.io.ByteArrayOutputStream

class PointSerializer(topic: String, schemaRegistryURL: String) extends KafkaRecordSerializationSchema[PointAvro] {

  private val serialVersionUID                                     = -4284080856874185929L
  @transient private var avroSerializer: Serde.AvroValueSerialiser = _

  override def open(
      context: SerializationSchema.InitializationContext,
      sinkContext: KafkaRecordSerializationSchema.KafkaSinkContext
  ): Unit =
    // This is very important, we initialize once the serializer to reduce overhead and push the schema
    // to schema registry only once, instead of each time a record is processed.
    avroSerializer = Serde.AvroValueSerialiser(schemaRegistryURL)

  /** Serialize a PointAvro using avro4s. But this does not serialize the object as expected by the confluent
    * deserializer.
    *
    * @param point
    * @return
    */
  private def serializeWithAvro4s(point: PointAvro): ProducerRecord[Array[Byte], Array[Byte]] = {
    val output = new ByteArrayOutputStream()
    val bin    = AvroOutputStream.data[PointAvro].to(output).build()
    bin.write(point)
    bin.flush()
    bin.close()
    new ProducerRecord[Array[Byte], Array[Byte]](topic, point.id.toString().getBytes(), output.toByteArray())
  }

  /** Serialize the PointAvro using the Confluent serializer
    *
    * @param point
    * @return
    */
  private def serializeWithConfluent(point: PointAvro): ProducerRecord[Array[Byte], Array[Byte]] = {
    val value = avroSerializer.serialize[PointAvro](topic, point)
    new ProducerRecord[Array[Byte], Array[Byte]](topic, point.id.toString().getBytes(), value)
  }

  override def serialize(
      point: PointAvro,
      context: KafkaRecordSerializationSchema.KafkaSinkContext,
      timestamp: java.lang.Long
  ): ProducerRecord[Array[Byte], Array[Byte]] =
    serializeWithConfluent(point)

}
