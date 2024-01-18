package main

import org.apache.avro.specific.SpecificRecord

class ProducerImpl() extends Producer[ProducerParams]:

    def init(): IO[Unit] = ???

    def produce(value: ProducerParams): Unit = 
        ???