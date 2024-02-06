package infrastructure
package grpc

import scala.concurrent.Future
import scala.concurrent.ExecutionContextExecutor
import akka.grpc.scaladsl.Metadata

import akka.actor.typed.ActorSystem

import akka.grpc.GrpcServiceException

import util.*
import entities.person.DataModel.*
import services.person.PersonService

import com.google.rpc.Code
import scala.util.Failure
import scala.util.Success

import api.event_sourced.managment.grpc.*
import api.common.*

import akka.Done

import ProtobufErrorsBuilder.*
import transformers.CommonTransformers.given
import transformers.PersonTransformers.given

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.protobufs.*
// import io.scalaland.chimney.javacollections.*

import java.util.UUID

// ProjectionManagement
// https://doc.akka.io/docs/akka-projection/current/management.html
// https://doc.akka.io/api/akka-projection/1.5.1/akka/projection/scaladsl/ProjectionManagement.html
import akka.projection.scaladsl.ProjectionManagement

import akka.persistence.query.Offset
import akka.projection.ProjectionId
import akka.persistence.query.Sequence
import akka.projection.internal.protobuf.ProjectionMessages.IsPaused

trait ProjectionsManagmentGrpc(
                            using system: ActorSystem[Nothing]):
    this: CommonGrpc =>

    given ExecutionContextExecutor = system.executionContext

    // transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues

    def projectionClearOffset(in: ProjectionIdRequest, metadata: Metadata): Future[Response] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val res: Future[Done] = ProjectionManagement(system).clearOffset(projectionId)
        res.map(
          _ => Response("Ok")
        )

    def projectionUpdateOffset(in: ProjectionUpdateOffsetRequest, metadata: Metadata): Future[Response] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val currentOffset: Future[Option[Sequence]] = ProjectionManagement(system).getOffset[Sequence](projectionId)
        val res = currentOffset.map {
          case Some(s) => ProjectionManagement(system).updateOffset[Sequence](projectionId, Sequence(s.value + 1))
          case None    => Done
        }
        res.map(
          _ => Response("Ok")
        )

    def projectionPause(in: ProjectionIdRequest, metadata: Metadata): Future[Response] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val mgmt = ProjectionManagement(system)
        val res = mgmt.pause(projectionId)
        res.map(
          _ => Response("Ok")
        )

    def projectionResume(in: ProjectionIdRequest, metadata: Metadata): Future[Response] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val mgmt = ProjectionManagement(system)
        val res = mgmt.resume(projectionId)
        res.map(
          _ => Response("Ok")
        )

    def projectionGetOffset(in: ProjectionIdRequest, metadata: Metadata): Future[OffsetResponse] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val currentOffset: Future[Option[Sequence]] = ProjectionManagement(system).getOffset[Sequence](projectionId)
        currentOffset.map {
          case Some(s) => OffsetResponse(Some(s.value))
          case None    => OffsetResponse(None)
        }

    def projectionIsPaused(in: ProjectionIdRequest, metadata: Metadata): Future[IsPausedResponse] =
        val projectionId = ProjectionId(in.id.projectionTag, in.id.targetTag)
        val res: Future[Boolean] = ProjectionManagement(system).isPaused(projectionId)
        res.map(IsPausedResponse(_))
