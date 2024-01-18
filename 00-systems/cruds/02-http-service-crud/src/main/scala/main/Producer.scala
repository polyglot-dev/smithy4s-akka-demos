package main

trait Producer[T]:
    def produce(value: T): Unit
    // def start() = ???

import org.apache.avro.specific.SpecificRecord

case class ProducerParams(topicName: String, key: String, value: SpecificRecord, eventType: String)
