package infrastructure
package http

import cats.data.EitherT

// format: off
trait ServiceError extends Throwable

trait RequestError  extends ServiceError
trait DBError       extends ServiceError
trait ServerError   extends ServiceError
trait SecurityError extends ServiceError


type Result[A] = EitherT[IO, ServiceError, A]

case class ServiceUnavailable(code: String, title: String, message: String) extends Throwable(message), ServerError
case class Conflict(code: String, title: String, message: String)           extends Throwable(message), DBError
case class BadRequest(code: String, title: String, message: String)         extends Throwable(message), RequestError
case class NotFound(code: String, title: String, message: String)           extends Throwable(message), DBError
case class InternalServer(code: String, title: String, message: String)     extends Throwable(message), ServerError
case class Unauthorized(code: String, title: String, message: String)       extends Throwable(message), SecurityError
case class Forbidden(code: String, title: String, message: String)          extends Throwable(message), SecurityError
// format: on

object ErrorsBuilder:

    def serviceUnavailableError
      (message: String)
      : ServiceUnavailable = ServiceUnavailable("LOYA503", "service-unavailable", message)

    def conflictError(message: String): Conflict = Conflict("LOYA409", "conflict", message)

    def badRequestError(message: String): BadRequest = BadRequest("LOYA400", "bad-request", message)

    def notFoundError(message: String): NotFound = NotFound("LOYA404", "not-found", message)
    
    def internalServerError(message: String): InternalServer = InternalServer("LOYA500", "internal-error", message)

    def unauthorizedError(message: String): Unauthorized = Unauthorized("LOYA401", "unauthorized", message)

    def forbiddenError(message: String): Forbidden = Forbidden("LOYA403", "forbidden", message)
