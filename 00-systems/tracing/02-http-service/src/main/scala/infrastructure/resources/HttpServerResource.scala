package infrastructure
package resources


import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import io.opentelemetry.api.trace.Tracer

import main.Configs.*
import types.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server

import io.grpc.ManagedChannel

class HttpServerResource(
                           service: AdvertiserService[IO], logger: IzLogger, channel: ManagedChannel, tracer: Tracer)(using
                           config: HttpServerConfig):

    def resource: Resource[IO, Server] = ServerRoutes(service, Some(logger), channel, tracer).all
      .flatMap:
          routes =>
              EmberServerBuilder
                .default[IO]
                .withPort(Port.fromInt(config.port).get)
                .withHttpApp(routes.orNotFound)
                .build
