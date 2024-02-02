package infrastructure
package grpc

import scala.concurrent.ExecutionContextExecutor
import akka.actor.typed.ActorSystem
import services.person.PersonService
import api.event_sourced.managment.grpc.*
import _root_.infrastructure.grpc.EntitiesManagmentGrpc

import services.person.PersonService
class ManagmentGRPCServerImpl(personService: PersonService)(
                             using system: ActorSystem[Nothing])
    extends ManagmentServicePowerApi,
      CommonGrpc(),
      ProjectionsManagmentGrpc(),
      EventsManagmentGrpc(),
      EntitiesManagmentGrpc(personService)(using system.executionContext)
