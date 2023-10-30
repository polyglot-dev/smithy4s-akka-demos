package main

import Configs.*

import domain.types.*
import infrastructure.types.*

import infrastructure.repositories.*
import infrastructure.http.services.*

import doobie.util.ExecutionContexts

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory
import org.http4s.server.Server

import logstage.{ ConsoleSink, IzLogger, Trace }
import izumi.logstage.api.routing.StaticLogRouter
import infrastructure.resources.*
import infrastructure.resources.PostgresResource

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.Tracer


object DI:

    val configModule =
      new ConfigModuleDef:
          makeConfig[DBConfig]("hikariTransactor")
          makeConfig[HttpServerConfig]("httpServer")
          makeConfig[GrpcClientConfig]("grpcClient")

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
              (res: PostgresResource,
                logger: IzLogger
                ) =>
                  res.resource.map(AdvertiserRepositoryImpl(_, Some(logger)))

          make[AdvertiserService[IO]].from:
              (
                repo: AdvertiserRepository[IO],
                logger: IzLogger
                ) =>
                  AdvertiserServiceImpl(repo, Some(logger))

          make[GrpcClientToWritesideResource].from:
              (config: GrpcClientConfig) =>
                  GrpcClientToWritesideResource(config)

          make[OpenTelemetry].from(OpentelemetryResource.init())

          make[Tracer].from:
              (
                openTelemetry: OpenTelemetry
                ) =>
                openTelemetry.getTracer("io.opentelemetry.example.JaegerExample")

          make[HttpServerResource].fromResource:
              (
                service: AdvertiserService[IO],
                config: HttpServerConfig,
                channel: GrpcClientToWritesideResource,
                tracer: Tracer,
                logger: IzLogger,
                ) =>
                  given HttpServerConfig = config
                  channel.resource.map(ch => HttpServerResource(service, logger, ch, tracer))

object App extends IOApp:

    def run(args: List[String]): IO[ExitCode] =
        import DI.*

        Injector[IO]().produceRun(mainModule ++ configModule):
            (
              httpServer: HttpServerResource) =>

                val program: IO[Unit] =
                  for
                    _ <- httpServer.resource.use(
                           (server: Server) =>
                             IO.never
                         )
                  yield ()

                program.as(
                  ExitCode.Success
                )

    end run
