package infrastructure
package http

import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dsl.io.*

import logstage.IzLogger

import smithy4s.hello.*
import types.*


import Converter.*

class ServerRoutes(
               logger: Option[IzLogger]
               ):
    private val mainRoutes: Resource[IO, HttpRoutes[IO]] = SimpleRestJsonBuilder.routes(HttpServerImpl2(logger).transform(Converter.toIO))
      .mapErrors(
        ex => ServiceUnavailableError("", "", ex.getMessage())
      )
    .resource

    private val healthCheck: HttpRoutes[IO] = HttpRoutes.of[IO]:
        // TODO: Check DB connection
        case GET -> Root / "alive" => Ok()
        case GET -> Root / "ready" => Ok()

    val all: Resource[IO, HttpRoutes[IO]] = mainRoutes.map(_ <+> healthCheck)
