package infrastructure
package grpc

import services.Configs.GrpcConfig

import akka.http.scaladsl.Http.ServerBinding

import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse

import akka.actor.typed.ActorSystem

import scala.concurrent.{ ExecutionContextExecutor, Future }

import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.actor.typed.ActorSystem

import scala.concurrent.{ ExecutionContextExecutor, Future }
import io.grpc.Status
import akka.actor.ActorSystem as ClassicActorSystem
import org.slf4j.{ Logger, LoggerFactory }
import io.grpc.StatusRuntimeException
import akka.grpc.{ GrpcServiceException, Trailers }
import akka.grpc.internal.{ GrpcMetadataImpl, MissingParameterException }
import akka.http.scaladsl.model.http2.PeerClosedStreamException

import scala.concurrent.ExecutionException
import com.google.rpc.Code

import api.eventSourced.grpc.*
import services.person.PersonService

import infrastructure.ProtobufErrorsBuilder.*

class GrpcApi(
           personService: PersonService)
             (
               using ec: ExecutionContextExecutor,
               sys: ActorSystem[Nothing],
               config: GrpcConfig):

    val logger: Logger = LoggerFactory.getLogger(getClass)

    private val INTERNAL = Trailers(Status.INTERNAL)
    private val INVALID_ARGUMENT = Trailers(Status.INVALID_ARGUMENT)

    def eHandler(system: ClassicActorSystem): PartialFunction[Throwable, Trailers] = {

      case e: com.google.protobuf.InvalidProtocolBufferException =>
        e.printStackTrace()
        logger.error("InvalidProtocolBufferException: [{}]", e)
        val error = badRequestError(e.getMessage())
        val grpcException = GrpcServiceException(Code.INVALID_ARGUMENT, e.getMessage(), Seq(error))
        Trailers(grpcException.status, grpcException.metadata)

      case e: ExecutionException               => INTERNAL
      case grpcException: GrpcServiceException => Trailers(grpcException.status, grpcException.metadata)
      case e: NotImplementedError              => Trailers(Status.UNIMPLEMENTED.withDescription(e.getMessage))
      case e: UnsupportedOperationException    => Trailers(Status.UNIMPLEMENTED.withDescription(e.getMessage))
      case _: MissingParameterException        => INVALID_ARGUMENT
      case e: StatusRuntimeException           =>
        val meta = Option(e.getTrailers).getOrElse(new io.grpc.Metadata())
        Trailers(e.getStatus, new GrpcMetadataImpl(meta))
      case ex: PeerClosedStreamException       =>
        logger.warn("Peer closed stream unexpectedly: {}", ex.getMessage)
        INTERNAL // nobody will receive it anyway
      case other                               =>
        logger.error("Unhandled error: [{}]", other.getMessage)
        INTERNAL
    }

    def init(): Future[ServerBinding] =
        val service: HttpRequest => Future[HttpResponse] = akka.grpc.scaladsl.ServiceHandler.concatOrNotFound(
          HomeServicePowerApiHandler.partial(
            implementation = GRPCServerImpl(personService),
            eHandler = eHandler
          ),
          akka.grpc.scaladsl.ServerReflection.partial(List(HomeService))
        )

        Http()
          .newServerAt(
            config.hostname,
            config.port,
          )
          .bind(service)
