package infrastructure
package http
package server
package common

import infrastructure.internal.common.*

type SmithyModelErrors =
  NotFoundError | BadRequestError | ServiceUnavailableError | ConflictError | InternalServerError | ForbiddenError | UnauthorizedError

trait CommonHTTP:

    def errorHandler(ex: java.lang.Throwable) = {
      ex match {
        case e: SmithyModelErrors => IO.raiseError(e)
        case _                    => IO.raiseError(ServiceUnavailableError(503, ex.getMessage()))
      }
    }
