
TOPIC_NAME=points_events
NUM_EVENTS=10

clean:
	sbt clean cleanFiles
	find . -name target -type d -exec rm -rf {} ;

fmt:
	sbt "scalafmtSbt; scalafmtAll"

assembly:
	sbt assembly

# Tasks #################################################################
show-avro-schema:
	sbt "flinkSerialization / runMain com.diego.ShowAvroSchema"

run-case-class:
	sbt "flinkSerialization / runMain com.diego.CaseClassSer ${NUM_EVENTS}"

run-avro-class:
	sbt "flinkSerialization / runMain com.diego.AvroClassSer ${NUM_EVENTS}"

run-pojo-class:
	sbt "flinkSerialization / runMain com.diego.PojoClassSer ${NUM_EVENTS}"

run-all-serializers:
	sbt "flinkSerialization / runMain com.diego.CaseClassSer ${NUM_EVENTS}; flinkSerialization / runMain com.diego.AvroClassSer ${NUM_EVENTS}; flinkSerialization / runMain com.diego.PojoClassSer ${NUM_EVENTS}"

run-avro-producer:
	sbt "flinkSerialization / runMain com.diego.AvroProducer ${NUM_EVENTS}"

# Kafka #################################################################
kafka-run:
	@docker-compose up kafka

create-topic:
	@docker-compose exec kafka bash -c "kafka-topics --topic ${TOPIC_NAME} --replication-factor 1 --partitions 1 --bootstrap-server localhost:9092 --config cleanup.policy=compact --create"

delete-topic:
	@docker-compose exec kafka bash -c "kafka-topics --topic ${TOPIC_NAME} --delete --bootstrap-server localhost:9092"

read-topic:
	@docker-compose exec kafka bash -c "kafka-console-consumer --topic ${TOPIC_NAME} --bootstrap-server localhost:9092 --from-beginning --property print.key=true"

avro_consumer="kafka-avro-console-consumer \
			--bootstrap-server kafka:9092 \
			--property schema.registry.url=http://schema-registry:8081 \
			--topic ${TOPIC_NAME} \
			--property print.key=true \
			--property print.value=true \
			--from-beginning \
			--value-deserializer io.confluent.kafka.serializers.KafkaAvroDeserializer \
			--key-deserializer org.apache.kafka.common.serialization.StringDeserializer"

read-avro-topic:
	docker-compose exec schema-registry bash -c ${avro_consumer}

read-kcat-topic:
	kcat -C -b localhost:29092 \
		-t ${TOPIC_NAME} \
		-r http://localhost:8081 \
		-s value=avro -e

kafka-describe-topic:
	@docker-compose exec kafka bash -c "kafka-topics --bootstrap-server kafka:9092 --describe --topics-with-overrides --topic ${TOPIC_NAME}"

kafka-clean: delete-topic create-topic

# Schema registry #######################################################
schema-run:
	@docker-compose up schema

schema-ls:
	curl localhost:8081/subjects

schema-push:
	curl -X POST -H "Content-Type: application/vnd.schemaregistry.v1+json" \
		--data '{"schema": "{\"type\":\"record\",\"name\":\"Point\",\"namespace\":\"com.diego.models\",\"fields\":[{\"name\":\"id\",\"type\":\"int\"},{\"name\":\"x\",\"type\":\"double\"},{\"name\":\"y\",\"type\":\"double\"}]}"}' \
		"http://localhost:8081/subjects/${TOPIC_NAME}-value/versions"

schema-clean:
	curl -X DELETE "http://localhost:8081/subjects/${TOPIC_NAME}-value?permanent=true"

reset-all: schema-clean kafka-clean
