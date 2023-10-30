package infrastructure
package resources

import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import main.Configs.*
import http.types.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server

class HttpServerResource(
                      service: AdvertiserService[IO],
                      logger: IzLogger)(using config: HttpServerConfig):

    def resource: Resource[IO, Server] = ServerRoutes(service, Some(logger)).all
      .flatMap:
          routes =>
              EmberServerBuilder
                .default[IO]
                .withPort(Port.fromInt(config.port).get)
                .withHttpApp(routes.orNotFound)
                .build
