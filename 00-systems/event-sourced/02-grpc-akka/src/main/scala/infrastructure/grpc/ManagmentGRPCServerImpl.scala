package infrastructure
package grpc

import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem

import services.person.PersonService

import api.event_sourced.managment.grpc.*

class ManagmentGRPCServerImpl(
                             using system: ActorSystem[Nothing])
    extends ManagmentServicePowerApi,
      CommonGrpc(),
      ProjectionsManagmentGrpc(),
      EventsManagmentGrpc()
