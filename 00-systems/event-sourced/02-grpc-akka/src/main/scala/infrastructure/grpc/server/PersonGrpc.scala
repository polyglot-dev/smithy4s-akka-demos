package infrastructure
package grpc

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.grpc.scaladsl.Metadata

import io.github.arainko.ducktape.*

import akka.grpc.GrpcServiceException

import util.*
import entities.person.DataModel.*
import services.person.PersonService

import com.google.rpc.Code
import scala.util.Failure
import scala.util.Success

import api.eventSourced.grpc.*

import akka.Done

import ProtobufErrorsBuilder.*
import transformers.CommonTransformers.given
import transformers.PersonTransformers.given

import java.util.UUID

trait PersonGrpc(personService: PersonService)(using ec: ExecutionContextExecutor):
    this: CommonGrpc =>

    def createPerson(in: CreatePersonRequest, metadata: Metadata): Future[CreatePersonResponse] =
        val id = UUID.randomUUID().toString
        personService.createPerson(id, in.to[Person]).transform:
            case Success(value) =>
              value match
                case _: Done        => Success(CreatePersonResponse(id))
                case e: ResultError => Failure(reportError(e))

            case Failure(exception) =>
              val error = serviceUnavailableError(exception.getMessage)
              Failure(
                GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))
              )

    def updatePerson(in: UpdatePersonRequest, metadata: Metadata): Future[UpdatePersonResponse] =
      personService.updatePerson(in.id, in.to[PersonUpdate]).transform:
          case Success(value) =>
            value match
              case _: Done        => Success(UpdatePersonResponse("ok"))
              case e: ResultError => Failure(reportError(e))

          case Failure(exception) =>
            val error = serviceUnavailableError(exception.getMessage)
            Failure(
              GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))
            )

    def getPerson
      (in: GetPersonRequest, metadata: Metadata)
      : Future[GetPersonResponse] = personService.getPerson(in.id).transform:
        case Success(value) =>
          value match
            case person: Person =>
              Success(
                person.into[GetPersonResponse]
                  .transform(
                    Field.const(_.unknownFields, scalapb.UnknownFieldSet.empty),
                  )
              )
            case e: ResultError => Failure(reportError(e))

        case Failure(exception) =>
          val error = serviceUnavailableError(exception.getMessage)
          Failure(
            GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))
          )
