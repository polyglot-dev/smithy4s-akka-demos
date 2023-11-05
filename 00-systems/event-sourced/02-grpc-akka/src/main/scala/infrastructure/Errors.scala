package infrastructure

object ProtobufErrorsBuilder:
    import api.common.*

    def serviceUnavailableError(message: String): ServiceUnavailableError = ServiceUnavailableError(
      code = "LOYA503",
      title = "service-unavailable",
      description = message
    )

    def conflictError(message: String): ConflictError = ConflictError(
      code = "LOYA409",
      title = "conflict",
      description = message
    )

    def badRequestError(message: String): BadRequestError = BadRequestError(
      code = "LOYA400",
      title = "bad-request",
      description = message
    )

    def notFoundError(message: String): NotFoundError = NotFoundError(
      code = "LOYA404",
      title = "not-found",
      description = message
    )

    def internalServerError(message: String): InternalServerError = InternalServerError(
      code = "LOYA500",
      title = "internal-error",
      description = message
    )

    def unauthorizedError(message: String): UnauthorizedError = UnauthorizedError(
      code = "LOYA401",
      title = "unauthorized",
      description = message
    )

    def forbiddenError(message: String): ForbiddenError = ForbiddenError(
      code = "LOYA403",
      title = "forbidden",
      description = message
    )
