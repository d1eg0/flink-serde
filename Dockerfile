FROM flink:1.14.4-scala_2.12-java11
ADD --chown=flink:flink target/scala-2.12/flink-serialization.jar /opt/flink/usrlib/artifacts/flink-serialization.jar
