package infrastructure
package http

import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.dsl.io.*

import io.opentelemetry.api.trace.Tracer

import logstage.IzLogger

import io.grpc.ManagedChannel

import smithy4s.hello.*
import types.*

class ServerRoutes(
               service: AdvertiserService[IO], logger: Option[IzLogger],channel: ManagedChannel, tracer: Tracer
               ):
    private val mainRoutes: Resource[IO, HttpRoutes[IO]] = SimpleRestJsonBuilder.routes(HttpServerImpl(service, logger, channel, tracer))
    //   .mapErrors(
    //     ex => ServiceUnavailableError(503, ex.getMessage())
    //   )
    .resource

    private val healthCheck: HttpRoutes[IO] = HttpRoutes.of[IO]:
        // TODO: Check DB connection
        case GET -> Root / "alive" => Ok()
        case GET -> Root / "ready" => Ok()

    val all: Resource[IO, HttpRoutes[IO]] = mainRoutes.map(_ <+> healthCheck)
