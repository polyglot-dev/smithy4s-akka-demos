package main

import Configs.*

import com.typesafe.config.ConfigFactory
import scala.concurrent.duration.*

import org.apache.avro.specific.SpecificRecord

import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.kafka.CommitterSettings
import com.typesafe.config.Config

import akka.actor.typed.*
import scala.concurrent.*
import akka.actor.typed.scaladsl.*
import akka.actor.typed.scaladsl.AskPattern.*

import akka.NotUsed
import akka.Done
import akka.actor.typed.{ DispatcherSelector, Terminated }

import akka.kafka.{ ConsumerSettings, Subscriptions }
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Committer
import akka.kafka.scaladsl.Consumer.DrainingControl

import akka.kafka.ConsumerMessage.CommittableMessage
import akka.kafka.CommitterSettings

import akka.stream.scaladsl.Sink

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.BytesDeserializer

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
import akka.stream.scaladsl.Source
import akka.kafka.scaladsl.Consumer.Control
import akka.kafka.ConsumerMessage.CommittableOffset
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.utils
import scala.util.*

object App:

    def apply(): Behavior[NotUsed] = Behaviors.setup {
      context =>
        Behaviors.receiveSignal {
          case (_, Terminated(_)) => Behaviors.stopped
        }
    }

    def main(args: Array[String]): Unit =
        given sys: ActorSystem[_] = ActorSystem(App(), "actor-system")
        given ec: scala.concurrent.ExecutionContext = sys.executionContext

        val config: Config = sys.settings.config.getConfig("akka.kafka.consumer")

        val committerSettings: CommitterSettings = CommitterSettings(sys)

        val messageProcessing: SpecificRecord => Future[Int] =
          (record: SpecificRecord) =>
              println("record: ")
              Future.successful(1)
        val messageProcessingt: utils.Bytes => Future[Int] =
          (record: utils.Bytes) =>
              println("record: ")
              Future.successful(1)

        val topic = "campaigns.advertiser-update.v1"
        val groupId = "group1"
        val kafkaConfig = KafkaConfig("http://0.0.0.0:18081")

        val resumeOnParsingException = ActorAttributes.supervisionStrategy {
          case ex: Exception =>
            ex.printStackTrace()
            Supervision.Resume
          // case _ => Supervision.stop
        }

        val kafkaAvroSerDeConfig = Map[String, Any](
          AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG -> kafkaConfig.schemaRegistryUrl,
          KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG -> true.toString,
          // AbstractKafkaAvroSerDeConfig.AUTO_REGISTER_SCHEMAS -> false.toString,
          AbstractKafkaAvroSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY -> "registry.strategy.RecordSubjectStrategy"
        )

        val kafkaAvroDeserializer = new KafkaAvroDeserializer()
        kafkaAvroDeserializer.configure(kafkaAvroSerDeConfig.asJava, false)
        val deserializer = kafkaAvroDeserializer.asInstanceOf[Deserializer[SpecificRecord]]

        val consumerSettings: ConsumerSettings[String, utils.Bytes] = {

          ConsumerSettings(config, new StringDeserializer, new BytesDeserializer)
            .withGroupId(groupId)
            .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
        }

        Consumer
          .committableSource(
            consumerSettings,
            Subscriptions.topics(topic)
          )
          .mapAsync(1):
              (msg: CommittableMessage[String, utils.Bytes]) =>
                  val m = msg.record.value

                  Try { deserializer.deserialize(topic, m.get()) } match
                    case Success(value)     =>
                      messageProcessing(value)
                        .map:
                            _ =>
                                msg.committableOffset
                    case Failure(exception) =>
                      println("dropped message")
                      exception.printStackTrace()
                      Future { msg.committableOffset }
          .via(Committer.flow(committerSettings.withMaxBatch(10)))
          .withAttributes(resumeOnParsingException)
          .toMat(Sink.seq)(DrainingControl.apply)
          .run()
