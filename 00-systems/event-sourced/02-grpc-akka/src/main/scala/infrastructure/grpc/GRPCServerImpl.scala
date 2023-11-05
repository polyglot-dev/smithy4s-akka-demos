package infrastructure
package grpc

import scala.concurrent.ExecutionContextExecutor

import services.person.PersonService

import api.eventSourced.grpc.*

class GRPCServerImpl(
                    personService: PersonService)(using ec: ExecutionContextExecutor)
    extends HomeServicePowerApi,
      CommonGrpc(),
      PersonGrpc(personService)
