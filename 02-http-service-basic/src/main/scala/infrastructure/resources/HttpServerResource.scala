package infrastructure
package resources


import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import main.Configs.*
import types.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server

class HttpServerResource(
                           logger: IzLogger)(using
                           config: HttpServerConfig):

    def resource: Resource[IO, Server] = ServerRoutes(Some(logger)).all
      .flatMap:
          routes =>
              EmberServerBuilder
                .default[IO]
                .withPort(Port.fromInt(config.port).get)
                .withHttpApp(routes.orNotFound)
                .build
