package main

import fs2.kafka.vulcan.{avroSerializer}
import fs2.kafka.vulcan.{Auth, AvroSettings, SchemaRegistryClientSettings}
import org.apache.avro.specific.SpecificRecord

import _root_.io.confluent.kafka.serializers.KafkaAvroSerializer
import _root_.io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.avro.io.*

import org.apache.kafka.common.serialization.Serializer as KSerializer

import infrastructure.Handler

import org.apache.avro.Schema

import fs2.kafka.*

import IO.asyncForIO

class ProducerAdvertiserImpl(writerSchema: Schema, handler: Handler[ProducerParams]) extends Producer[ProducerParams]:

    val avroSettings =
      AvroSettings {
        SchemaRegistryClientSettings[IO]("http://localhost:18081")
      }
      // .withAutoRegisterSchemas(true)
      .withAutoRegisterSchemas(false)
      .withValueSubjectNameStrategy("registry.strategy.RecordSubjectStrategy")

    val kser: IO[(KafkaAvroSerializer, SchemaRegistryClient)] = avroSettings.createAvroSerializer(false, Some(writerSchema))
    val ttA = for a <- kser yield a._1.asInstanceOf[KSerializer[SpecificRecord]]
    
    def init(): IO[Unit] =  for {

                      queue <-  Channel.unbounded[IO, ProducerParams]
                      tttt <- ttA
                      
                      producerSettings <- IO{ProducerSettings(                                       
                                              keySerializer = Serializer[IO, String],
                                              valueSerializer = Serializer.delegate(tttt),
                                            )
                                            .withBootstrapServers("localhost:19092")
                                            }
                                            
                      _ <- IO(handler.queue = Some(queue))
                      
                      runningQueue <-   queue
                              .stream
                              // .covary[IO]
                              // .evalTap(ev => {
                              //   IO.println(s"Got $ev")
                              // })
                            //  .through(pTransform)
                             .map{
                              value =>
                                  IO.println(s"EventStream =>>>>>>>>>>>> $value")
                                  val record = ProducerRecord(value.topicName, value.key, value.value)
                                  ProducerRecords.one(record)
                             }
                             .through(KafkaProducer.pipe(producerSettings))
                             .compile
                             .drain
                       } yield runningQueue
    
    def produce(value: ProducerParams): IO[Either[Channel.Closed, Unit]] = 
        handler.queue.get.send(value)