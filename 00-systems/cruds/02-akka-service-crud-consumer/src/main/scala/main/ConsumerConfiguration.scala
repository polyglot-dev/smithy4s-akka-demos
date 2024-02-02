package main

import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Committer
import akka.kafka.scaladsl.Consumer.DrainingControl

import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.CommitterSettings

import akka.stream.scaladsl.Sink

import org.apache.kafka.common.serialization.StringDeserializer

import com.typesafe.config.Config

import io.confluent.kafka.serializers.{ AbstractKafkaAvroSerDeConfig, KafkaAvroDeserializer }
import org.apache.avro.specific.SpecificRecord
import org.apache.kafka.common.serialization.*
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import scala.concurrent.Future
import akka.actor.typed.ActorSystem
import scala.concurrent.ExecutionContextExecutor
import akka.Done
import scala.jdk.CollectionConverters.*

import org.apache.kafka.clients.consumer.ConsumerConfig
import Configs.*

import org.slf4j.{ Logger, LoggerFactory }
import akka.stream.ActorAttributes
import akka.stream.Supervision

class ConsumerConfiguration[F](
                            config: Config,
                            committerSettings: CommitterSettings,
                            groupId: String,
                            topic: String,
                            messageProcessing: SpecificRecord => Future[Int])
                              (
                                using sys: ActorSystem[Nothing],
                                kafkaConfig: KafkaConfig):
    given ec: ExecutionContextExecutor = sys.executionContext
    val logger: Logger = LoggerFactory.getLogger(getClass)

    def run(): DrainingControl[Seq[Done]] =

        val resumeOnParsingException = ActorAttributes.supervisionStrategy {
          case ex: Exception =>
            ex.printStackTrace()
            Supervision.Resume
          // case _ => Supervision.stop
        }

        val kafkaAvroSerDeConfig = Map[String, Any](
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> kafkaConfig.schemaRegistryUrl,
          KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString,
          AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS -> false.toString,
          AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> "registry.strategy.RecordSubjectStrategy"
        )
        val consumerSettings: ConsumerSettings[String, SpecificRecord] = {
          val kafkaAvroDeserializer = new KafkaAvroDeserializer()
          kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, false)
          val deserializer = kafkaAvroDeserializer.asInstanceOf[Deserializer[SpecificRecord]]

          ConsumerSettings(config, new StringDeserializer, deserializer)
            .withGroupId(groupId)
            .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }

        Consumer
          .committableSource(
            consumerSettings,
            Subscriptions.topics(topic)
          )
          .mapAsync(1):
              (msg: CommittableMessage[String, SpecificRecord]) =>
                  val m = msg.record.value
                  messageProcessing(m)
                    .map:
                        _ =>
                            msg.committableOffset
          .via(Committer.flow(committerSettings.withMaxBatch(10)))
          .withAttributes(resumeOnParsingException)
          .toMat(Sink.seq)(DrainingControl.apply)
          .run()
