package infrastructure
package grpc

import services.Configs.GrpcConfig

import api.eventSourced.grpc.*
import services.person.PersonService

import akka.http.scaladsl.Http.ServerBinding

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse

import akka.actor.typed.ActorSystem

import scala.concurrent.{ ExecutionContextExecutor, Future }

class GrpcApi(
           personService: PersonService)
             (
               using ec: ExecutionContextExecutor,
               sys: ActorSystem[Nothing],
               config: GrpcConfig):

    def init(): Future[ServerBinding] =
        val service: HttpRequest => Future[HttpResponse] = HomeServicePowerApiHandler
          .withServerReflection(
            GRPCServerImpl(personService)
          )

        Http()
          .newServerAt(
            config.hostname,
            config.port,
          )
          .bind(service)
