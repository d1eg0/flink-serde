package com.diego.serde

// Thanks to https://github.com/spi-x-i/flink-hybrid-source/

import io.confluent.kafka.serializers.{
  AbstractKafkaSchemaSerDeConfig,
  KafkaAvroDeserializer,
  KafkaAvroDeserializerConfig,
  KafkaAvroSerializer
}
import org.apache.kafka.common.serialization.{ Deserializer, Serializer }

import scala.collection.JavaConverters._

object SubjectNameStrategies {

  sealed trait SubjectNameStrategy

  case object TopicNameStrategy       extends SubjectNameStrategy
  case object RecordNameStrategy      extends SubjectNameStrategy
  case object TopicRecordNameStrategy extends SubjectNameStrategy

}

object Serde {

  import SubjectNameStrategies._

  /** Utility Serializer based on underlying [[KafkaAvroSerializer]].
    *
    * This utility relies on Confluent Schema Registry responding at [[schemaRegistryURL]]. The serialiser will publish
    * subjects following [[io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy]]; the schema obtained
    * from the concrete instance will be pushed as a subject automatically
    *
    * @param schemaRegistryURL
    *   The URL where the Confluent Schema Registry listens
    */
  final class AvroValueSerialiser(schemaRegistryURL: String, subjectNameStrategy: SubjectNameStrategy) {

    private val strategy = getStrategy(subjectNameStrategy)

    private val props = Map(
      AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> strategy.getName,
      AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS       -> true.toString,
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG  -> schemaRegistryURL
    ).asJava

    private val serialiser = new KafkaAvroSerializer()
    serialiser.configure(props, false)

    def serialize[T](topic: String, msg: T): Array[Byte] = serialiser.serialize(topic, msg)

    def getUnderlying[T]: Serializer[T] = serialiser.asInstanceOf[Serializer[T]]
  }

  object AvroValueSerialiser {

    def apply(schemaRegistryURL: String): AvroValueSerialiser =
      new AvroValueSerialiser(schemaRegistryURL, SubjectNameStrategies.TopicNameStrategy)

  }

  /** Utility Deserialiser based on underlying [[KafkaAvroDeserializer]].
    *
    * This utility relies on Confluent Schema Registry responding at [[schemaRegistryURL]]. The deserialiser will pull
    * subjects following [[io.confluent.kafka.serializers.subject.strategy.SubjectNameStrategy]]x strategy and it is
    * configurable in terms of return type, due to [[useSpecificReader]]
    *
    * @param schemaRegistryURL
    *   The URL where the Confluent Schema Registry listens
    * @param useSpecificReader
    *   If true, a [[org.apache.avro.specific.SpecificRecord]] will be returned (if exists), a
    *   [[org.apache.avro.generic.GenericRecord]] otherwise
    */
  final class AvroValueDeserialiser(
      schemaRegistryURL: String,
      useSpecificReader: Boolean,
      subjectNameStrategy: SubjectNameStrategy
  ) {

    private val strategy = getStrategy(subjectNameStrategy)

    private val props = Map(
      AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> strategy.getName,
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG  -> schemaRegistryURL,
      KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG    -> useSpecificReader.toString
    ).asJava

    private val deserialiser = new KafkaAvroDeserializer()
    deserialiser.configure(props, false)

    def deserialize[T](topic: String, data: Array[Byte]): T = deserialiser.deserialize(topic, data).asInstanceOf[T]

    def getUnderlying[T]: Deserializer[T] = deserialiser.asInstanceOf[Deserializer[T]]
  }

  object AvroValueDeserialiser {

    def apply(schemaRegistryURL: String): AvroValueDeserialiser =
      new AvroValueDeserialiser(schemaRegistryURL, useSpecificReader = true, SubjectNameStrategies.TopicNameStrategy)
  }

  private def getStrategy(strategy: SubjectNameStrategy) =
    strategy match {
      case TopicNameStrategy       => classOf[io.confluent.kafka.serializers.subject.TopicNameStrategy]
      case RecordNameStrategy      => classOf[io.confluent.kafka.serializers.subject.RecordNameStrategy]
      case TopicRecordNameStrategy => classOf[io.confluent.kafka.serializers.subject.TopicRecordNameStrategy]
    }

}
