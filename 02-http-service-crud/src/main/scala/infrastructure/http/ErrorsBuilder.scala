package infrastructure
package http

// import _root_.infrastructure.internal.common.*

case class ServiceUnavailable(code: String, title: String, message: String) extends Throwable(message)
case class Conflict(code: String, title: String, message: String)           extends Throwable(message)
case class BadRequest(code: String, title: String, message: String)         extends Throwable(message)
case class NotFound(code: String, title: String, message: String)           extends Throwable(message)

object ErrorsBuilder:

    def serviceUnavailableError
      (message: String)
      : ServiceUnavailable = ServiceUnavailable("LOYA503", "service-unavailable", message)

    def conflictError(message: String): Conflict = Conflict("LOYA409", "conflict", message)

    def notFoundError(message: String): NotFound = NotFound("LOYA404", "not-found", message)

    def badRequestError(message: String): BadRequest = BadRequest("LOYA400", "bad-request", message)
