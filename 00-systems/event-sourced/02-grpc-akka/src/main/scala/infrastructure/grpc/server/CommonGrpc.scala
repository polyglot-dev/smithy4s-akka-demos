package infrastructure
package grpc

import akka.grpc.GrpcServiceException

import util.*
import com.google.rpc.Code

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import ProtobufErrorsBuilder.*

trait CommonGrpc:

    val logger: Logger = LoggerFactory.getLogger(getClass)

    def reportError(e: ResultError): GrpcServiceException =
      e match
        case ResultError(TransportError.NotFound, msg) =>
          val error = notFoundError(msg)
          GrpcServiceException(Code.NOT_FOUND, "Message not found", Seq(error))

        case ResultError(TransportError.InternalServerError, msg) =>
          val error = serviceUnavailableError(msg)
          GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))

        case ResultError(_, msg) =>
          val error = serviceUnavailableError(msg)
          GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))

    def respServiceUnavailable() =
        val error = serviceUnavailableError("error")
        GrpcServiceException(Code.INTERNAL, "Internal error", Seq(error))
