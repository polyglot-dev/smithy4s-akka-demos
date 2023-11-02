package main

import Configs.*

import domain.types.*
import infrastructure.types.*

import doobie.util.ExecutionContexts

import distage.Injector
import distage.ModuleDef
import distage.config.{ AppConfig, ConfigModuleDef }
import com.typesafe.config.ConfigFactory
import org.http4s.server.Server

import logstage.{ ConsoleSink, IzLogger, Trace }
import izumi.logstage.api.routing.StaticLogRouter
import infrastructure.resources.*


object DI:

    val configModule =
      new ConfigModuleDef:
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

          make[HttpServerResource].from:
              (
                config: HttpServerConfig,
                logger: IzLogger,
                ) =>
                  given HttpServerConfig = config
                  HttpServerResource(logger)

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
