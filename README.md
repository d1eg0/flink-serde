# Serialization stuff with Flink

This is just a Flink toy project to play with Flink internal serialization using Scala.

I was having some doubts when dealing Scala case classes and Flink internal serialization. I decided to start this repo that hopefully will help me to undertand better every step of the serialization in a Flink streaming job. 

In particular, I'll play with:
- Inter operator and state object serialization.
- Kafka sink object serialization.

Serialization has a very high impact on latency and throughtput on any stream processor. The objects transfered between Flink operators are serialized, hence the size of the serialized object and the serialization process overhead are important points that will impact on our streaming process performance.

# Our data model to serialize

This is the base data model we want to serialize:

```scala
package com.diego.models

final case class Point(id: Int, x: Option[Double], y: Option[Double])
```

The corresponding Avro schema of this case class is:

```json
{
    "type": "record",
    "name": "PointAvro",
    "namespace": "com.diego.models",
    "fields": [
        {
            "name": "id",
            "type": "int"
        },
        {
            "name": "x",
            "type": [
                "null",
                "double"
            ]
        },
        {
            "name": "y",
            "type": [
                "null",
                "double"
            ]
        }
    ]
}
```

You can find it here [src/main/avro/point.avsc](src/main/avro/point.avsc).

# Internal Flink serialization

A very important point we can read at the Flink documentation:
> Note that when a user-defined data type can’t be recognized as a POJO type, it must be processed as GenericType and serialized with Kryo.

[Kryo serializer is slow compared to Avro or Protobuf](https://flink.apache.org/news/2020/04/15/flink-serialization-tuning-vol-1.html), so we'd like to avoid Kryo as much as possible:

> The default fallback from POJO to Kryo reduces performance by 75%

To disable Kryo you just add this statement:

```scala
val env = StreamExecutionEnvironment.getExecutionEnvironment
env.getConfig().disableGenericTypes()
```

Once Kryo is disabled, if your object is not serialized by other serializer will throw the following exception:

```bash
Generic types have been disabled in the ExecutionConfig and type com.diego.models.Point is treated as a generic type.
```

This error means the objects would be processed as GenericType and serialized using Kryo. And this is what we want, to identify a serialization bottleneck in our code.

## Generating the Avro schema

You can define the *.avsc file (AVro SChema) from scratch, but I'm lazy so I used `com.sksamuel.avro4s.AvroSchema` to generate it from a case class. See [ShowAvroSchema.scala](src/main/scala/com/diego/ShowAvroSchema.scala).

Once we have the Avro schema we can create the case class from it on compile time using [sbt-avrohugger](https://github.com/julianpeeters/sbt-avrohugger).

Once compiled, we can refer to our case class from the generated source by sbt-avrohugger on our code. And this case class won't throw any error when disabling Kryo serialization because is being serialized by Avro, our case class is extending `org.apache.avro.specific.SpecificRecordBase`.

## How to force Avro serialization and disable generic types

The next statements will force disabling generic types and use Avro. Hence, in case our class does not fall
in a proper serialization will throw an exception

```scala
  val env = StreamExecutionEnvironment.getExecutionEnvironment
  env.getConfig().disableGenericTypes()
  env.getConfig().enableForceAvro()
```

# Object serialization when writing to a Kafka topic

In order to serialize an object to write to Kafka the first try was using [avro4s](https://github.com/sksamuel/avro4s) but the resulting array of bytes is not the same as the Confluent serializer. I think (and based on avro4s docs) this is because avro4s serializes the object as a Generic Record, when the case class extends from specific record, so deserialization with the Kafka console consumer fails.

Moreover, avro4s has not an integration with the Confluent Schema registry.

For the second attempt I used the Confluent serializer `io.confluent.kafka.serializers`, and based on this repo [https://github.com/spi-x-i/flink-hybrid-source/](https://github.com/spi-x-i/flink-hybrid-source/) and it works perfectly.


# How can I run all this

## Internal serializers

To run the different serializers:

```bash
make run-all-serializers # run all of them
```

## Kafka sink with Avro

First you need a broker, zookeeper and schema registry running on your host.
You can just run `docker-compose up -d` to run these components. But I prefer to check the logs without detaching:

```bash
make kafka-run
make schema-run #run this in another panel/terminal
```

Then create the Kafka topic `points_events`:

```bash 
make create-topic
```

To run the Avro Kafka producer:

```bash
make run-avro-producer 
```

In another terminal you can read the Avro records from the topic running:

```bash
make read-avro-topic
```


# Benchmarks (WIP)

This section will contain how to compare different approaches performance and performance results.

## Flame graphs

### Installation on MacOS

1. Download last release of [async-profiler](https://github.com/jvm-profiling-tools/async-profiler/releases)

2. Create symbolic links to access the profiler:

  ```bash
  ln -s ~/tools/async-profiler/profiler.sh $HOME/bin/profiler.sh
  ln -s ~/tools/async-profiler/build $HOME/bin/build
  ```

3. Identify the JVM process Id to profile:

  ```bash
  jps
  ```

4. Run the profiler:

  ```bash
  profiler.sh -d 60 -f flamegraph.html <process-id>
  ```

Flame graphs resources to understand them:

- https://www.brendangregg.com/flamegraphs.html
- [Java in Flames](https://netflixtechblog.com/java-in-flames-e763b3d32166)

Some graphs are located in [benchmark/flamegraph](benchmark/flamegraph).
