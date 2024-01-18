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

          make[Producer[ProducerParams]].from:
              (handler: Handler[ProducerParams]) =>
                  new ProducerAdvertiserImpl(ad.Advertiser.getClassSchema(), handler)

          make[Handler[ProducerParams]].from:
              () =>
                new Handler[ProducerParams]()

          make[AdvertiserService[Result]].from:
              (
                repo: AdvertiserRepository[IO],
                producer: Producer[ProducerParams],
                logger: IzLogger) =>
                  AdvertiserServiceImpl(repo, Some(producer), Some(logger))

          make[HttpServerResource].from:
              (
                service: AdvertiserService[Result],
                config: HttpServerConfig,
                logger: IzLogger) =>
                  given HttpServerConfig = config
                  HttpServerResource(service, logger)


import _root_.io.scalaland.chimney.dsl.*
import _root_.io.scalaland.chimney.{ partial, PartialTransformer, Transformer }

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

        Injector[IO]().produceRun(mainModule ++ configModule):
            (
              httpServer: HttpServerResource,
              producer: Producer[ProducerParams],
              ) =>
                val program: IO[Unit] =
                  for
                    _ <- httpServer.resource.use(
                           (server: Server) =>
                             IO.never
                         )
                  yield ()

                program.as(ExitCode.Success) &> producer.init().as(ExitCode.Success)

    end run
