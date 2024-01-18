package infrastructure
package resources

import org.http4s.implicits.*
import org.http4s.ember.server.*
import com.comcast.ip4s.*
import logstage.IzLogger

import main.Configs.*
import http.types.*

import com.comcast.ip4s.*

import infrastructure.http.ServerRoutes
import org.http4s.server.Server
import http.Result

class HttpServerResource(
                      service: AdvertiserService[Result],
                      logger: IzLogger)(using config: HttpServerConfig):

    def resource: Resource[IO, Server] = ServerRoutes(service, Some(logger)).all
      .flatMap:
          routes =>
              EmberServerBuilder
                .default[IO]
                .withHost(host"0.0.0.0")
                .withPort(Port.fromInt(config.port).get)
                .withHttpApp(routes.orNotFound)
                .build
