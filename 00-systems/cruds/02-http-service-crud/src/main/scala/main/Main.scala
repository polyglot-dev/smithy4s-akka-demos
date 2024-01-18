package main

import Configs.*

import domain.types.*

import infrastructure.repositories.*
import infrastructure.http.services.*
import infrastructure.http.types.*
import infrastructure.*

import doobie.util.ExecutionContexts

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory
import org.http4s.server.Server

import logstage.{ ConsoleSink, IzLogger, Trace }
import izumi.logstage.api.routing.StaticLogRouter
import infrastructure.resources.*

import fs2.kafka.*
import fs2.kafka.KafkaProducer.*

import fs2.kafka.*
import fs2.kafka.KafkaProducer.*
import cats.effect.std.*
import fs2.*
import cats.effect.*
import scala.concurrent.duration.*

import infrastructure.http.Result

import integration.serializers.*
import org.integration.avro.ad

import org.apache.avro.specific.SpecificRecord

object DI:

    val configModule =
      new ConfigModuleDef:
          makeConfig[DBConfig]("hikariTransactor")
          makeConfig[HttpServerConfig]("httpServer")

          make[AppConfig].from(AppConfig(
            ConfigFactory
              .defaultApplication().getConfig("app").resolve()
          ))

    val mainModule =
      new ModuleDef:

          make[IzLogger].from:
              () =>
                  val textSink = ConsoleSink.text(colored = true)
                  val sinks = List(textSink)
                  val res = IzLogger(Trace, sinks)
                  StaticLogRouter.instance.setup(res.router)
                  res

          make[Resource[IO, ExecutionContext]].from:
              (config: DBConfig) =>
                  ExecutionContexts.fixedThreadPool[IO](config.threadPoolSize)

          make[PostgresResource].fromResource:
              (ec: Resource[IO, ExecutionContext], config: DBConfig) =>
                  ec.map(PostgresResource(_, config))

          make[AdvertiserRepository[IO]].fromResource:
              (res: PostgresResource, logger: IzLogger) =>
                  res.resource.map(AdvertiserRepositoryImpl(_, Some(logger)))

          make[Handler[SpecificRecord]].from:
              () =>
                new Handler[SpecificRecord]()

          make[AdvertiserService[Result]].from:
              (
                repo: AdvertiserRepository[IO],
                ch: Handler[SpecificRecord],
                logger: IzLogger) =>
                  AdvertiserServiceImpl(repo, Some(ch), Some(logger))

          make[HttpServerResource].from:
              (
                service: AdvertiserService[Result],
                config: HttpServerConfig,
                logger: IzLogger) =>
                  given HttpServerConfig = config
                  HttpServerResource(service, logger)


import _root_.io.confluent.kafka.serializers.KafkaAvroSerializer
import _root_.io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.apache.avro.io.*
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.avro.specific.SpecificDatumReader


import org.apache.kafka.common.serialization.Serializer as KSerializer

import java.io.ByteArrayOutputStream
import _root_.io.scalaland.chimney.dsl.*
import _root_.io.scalaland.chimney.{ partial, PartialTransformer, Transformer }

import fs2.kafka.vulcan.{avroSerializer}
import fs2.kafka.vulcan.{Auth, AvroSettings, SchemaRegistryClientSettings}

// import cats.effect.Sync
import IO.asyncForIO
// import SyncIO.syncForSyncIO

object App extends IOApp:

    def run(args: List[String]): IO[ExitCode] =
        import DI.*
    
        transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues.enableBeanSetters.enableBeanGetters.enableInheritedAccessors

        given StatusToStatus: Transformer[Dtos.Status, ad.Status] with

            def transform(self: Dtos.Status): ad.Status =
              self match
                case Dtos.Status.ACTIVE            => ad.Status.ACTIVE
                case Dtos.Status.INACTIVE          => ad.Status.INACTIVE
                case Dtos.Status.PENDING           => ad.Status.PENDING
                case Dtos.Status.DELETED           => ad.Status.DELETED

        val avroSettings =
          AvroSettings {
            SchemaRegistryClientSettings[IO]("http://localhost:18081")
          }
          // .withAutoRegisterSchemas(true)
          .withAutoRegisterSchemas(false)
          .withValueSubjectNameStrategy("registry.strategy.RecordSubjectStrategy")

        val kser: IO[(KafkaAvroSerializer, SchemaRegistryClient)] = avroSettings.createAvroSerializer(false, Some(ad.Advertiser.getClassSchema()))
        val ttA = for a <- kser yield a._1.asInstanceOf[KSerializer[SpecificRecord]]

        Injector[IO]().produceRun(mainModule ++ configModule):
            (
              httpServer: HttpServerResource,
              handler: Handler[SpecificRecord],
              ) =>
                val streams = for {

                      queue <-  Channel.unbounded[IO, SpecificRecord]
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
                              .evalTap(ev => {
                                IO.println(s"Got $ev")
                              })
                              .map {
                                value =>
                                    IO.println(s"EventStream =>>>>>>>>>>>> $value")
                                    val record = ProducerRecord("campaigns.advertiser-update.v1", "key", value)
                                    ProducerRecords.one(record)
                             }
                             .through(KafkaProducer.pipe(producerSettings))
                             .compile
                             .drain
                       } yield runningQueue

                val program: IO[Unit] =
                  for
                    _ <- httpServer.resource.use(
                           (server: Server) =>
                             IO.never
                         )
                  yield ()

                program.as( ExitCode.Success) &> streams.as(ExitCode.Success)

    end run
