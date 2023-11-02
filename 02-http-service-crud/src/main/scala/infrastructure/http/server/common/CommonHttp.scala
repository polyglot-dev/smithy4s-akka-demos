package infrastructure
package http
package server
package common

// import infrastructure.internal.common.*
// type SmithyModelErrors =
//   NotFoundError | BadRequestError | ServiceUnavailableError | ConflictError | InternalServerError | ForbiddenError | UnauthorizedError


import ErrorsBuilder.*

type ModelErrors = NotFound | BadRequest | ServiceUnavailable | Conflict

trait CommonHTTP:

    def errorHandler(ex: java.lang.Throwable) = {
      ex match {
        // case e: SmithyModelErrors => IO.raiseError(e)
        case e: ModelErrors       => IO.raiseError(e)
        case _                    => IO.raiseError(serviceUnavailableError(ex.getMessage()))
      }
    }
