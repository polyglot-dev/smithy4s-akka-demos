package main

import Configs.*

import domain.types.*

import infrastructure.repositories.*
import infrastructure.http.services.*
import infrastructure.http.types.*

import doobie.util.ExecutionContexts

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory
import org.http4s.server.Server

import logstage.{ ConsoleSink, IzLogger, Trace }
import izumi.logstage.api.routing.StaticLogRouter
import infrastructure.resources.{ HttpServerResource, PostgresResource }

import infrastructure.http.Result

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

          make[AdvertiserService[Result]].from:
              (
                repo: AdvertiserRepository[IO],
                logger: IzLogger) =>
                  AdvertiserServiceImpl(repo, Some(logger))

          make[HttpServerResource].from:
              (
                service: AdvertiserService[Result],
                config: HttpServerConfig,
                logger: IzLogger) =>
                  given HttpServerConfig = config
                  HttpServerResource(service, logger)

object App extends IOApp:

    def run(args: List[String]): IO[ExitCode] =
        import DI.*

        Injector[IO]().produceRun(mainModule ++ configModule):
            (
              httpServer: HttpServerResource
            ) =>

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
