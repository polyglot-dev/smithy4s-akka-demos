package util

import akka.serialization.jackson.CborSerializable

case class ResultError(code: TransportError, message: String) extends CborSerializable

enum TransportError extends CborSerializable:
    case NotFound, BadRequest, InternalServerError, Unauthorized, Forbidden, Unknown
