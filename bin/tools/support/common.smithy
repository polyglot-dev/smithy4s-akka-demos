$version: "2"

namespace infrastructure.internal.common

//structure Request {}
//structure Response {}

@mixin
structure ErrorMixin {
  @required
  code: String
  @required
  title: String
  @required
  description: String
}

@httpError(400)
@error("client")
structure BadRequestError with [ErrorMixin]{}

@httpError(401)
@error("client")
structure UnauthorizedError with [ErrorMixin]{}

@httpError(403)
@error("client")
structure ForbiddenError with [ErrorMixin]{}

@httpError(404)
@error("client")
structure NotFoundError with [ErrorMixin]{}

@httpError(406)
@error("client")
structure NotAcceptableError with [ErrorMixin]{}

@httpError(408)
@error("client")
structure RequestTimeoutError with [ErrorMixin]{}

@httpError(409)
@error("client")
structure ConflictError with [ErrorMixin]{}

@httpError(413)
@error("client")
structure ContentTooLargeError with [ErrorMixin]{}

@httpError(416)
@error("client")
structure RangeNotSatisfiableError with [ErrorMixin]{}

@httpError(422)
@error("client")
structure UnprocessableContentError with [ErrorMixin]{}

@httpError(423)
@error("client")
structure LockedError with [ErrorMixin]{}

@httpError(451)
@error("client")
structure UnavailableForLegalReasonsError with [ErrorMixin]{}

@httpError(500)
@error("server")
structure InternalServerError with [ErrorMixin]{}

@httpError(501)
@error("server")
structure NotImplementedError with [ErrorMixin]{}

@httpError(503)
@error("server")
structure ServiceUnavailableError with [ErrorMixin]{}

@httpError(507)
@error("server")
structure InsufficientStorageError with [ErrorMixin]{}

