package infrastructure
package grpc

import akka.Done
import akka.actor.typed.ActorSystem
import akka.grpc.GrpcServiceException
import akka.grpc.scaladsl.Metadata

import akka.persistence.query.{Offset, Sequence}
import akka.projection.ProjectionId
import akka.projection.internal.protobuf.ProjectionMessages.IsPaused
import akka.projection.scaladsl.ProjectionManagement

import com.google.rpc.Code

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.protobufs.*

import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success}

import java.util.UUID

import api.common.*
import api.eventSourced.grpc.CreatePersonResponse
import api.event_sourced.managment.grpc.*

import infrastructure.ProtobufErrorsBuilder.serviceUnavailableError
import infrastructure.entities.person.DataModel.Person
import services.person.PersonService

import util.*

trait EntitiesManagmentGrpc(personService: PersonService)(
                            using ec: ExecutionContextExecutor):
    this: CommonGrpc =>

    def stopEntity(in: StopEntityRequest, metadata: Metadata)
    : Future[Response] = personService.stop(in.entityId).transform:
        case Success(value) =>
          value match
            case _: Done        => Success(Response("Ok"))
            case e: ResultError => Failure(reportError(e))

        case Failure(exception) =>
          val error = serviceUnavailableError(exception.getMessage)
          Failure(
            GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))
          )
