package main

trait Producer[T]:
    def produce(value: T): IO[Either[Channel.Closed, Unit]]
    def init(): IO[Unit]

import org.apache.avro.specific.SpecificRecord

case class ProducerParams(topicName: String, key: String, value: SpecificRecord)
