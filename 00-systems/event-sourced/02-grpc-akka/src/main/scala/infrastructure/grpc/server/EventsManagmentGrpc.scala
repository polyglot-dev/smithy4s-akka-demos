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

import doobie._
import doobie.implicits._
import doobie.util.ExecutionContexts
import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import fs2.Stream

trait EventsManagmentGrpc(
                       using system: ActorSystem[Nothing]):
    this: CommonGrpc =>

    // transparent inline given TransformerConfiguration[?] = TransformerConfiguration.default.enableDefaultValues
    // unsafe methods directly.

    import cats.effect.unsafe.implicits.global

    // given  system.executionContext

    val xa = Transactor.fromDriverManager[IO](
      driver = "org.postgresql.Driver",
      url = "jdbc:postgresql://127.0.0.1:5432/service_database",
      user = "duser",
      password = "dpass",
      logHandler = None
    )

    def eventsUpdateAll(in: EventsUpdateAllRequest, metadata: Metadata): Future[Response] =
        sql"select name from person"
          .query[String] // Query0[String]
          .to[List] // ConnectionIO[List[String]]
          .transact(xa) // IO[List[String]]
          .unsafeRunSync() // List[String]
          .take(5) // List[String]
          .foreach(println) // Unit
        // ???
        Future { Response("Ok") }(using system.executionContext)
